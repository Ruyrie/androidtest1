package com.heban.app.ui.discover.scan;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDestination;

import com.google.common.util.concurrent.ListenableFuture;
import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.databinding.FragmentScanBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * D02 拍照识别（原型 Scan.dc.html，技术准备文档 9.5 / 11.3）：CameraX 预览与拍照、闪光灯、识别模式、
 * 系统照片选择器（不需要存储权限）、识别记录底部弹窗。首次进入申请相机权限，被拒绝时页内说明并提供「去授权」，
 * 相册仍可用。拍照 / 选图前要求登录（游客可以打开本页），随后立即进入 D03 显示识别中。
 */
public class ScanFragment extends BaseFragment {
    private FragmentScanBinding binding;
    private ScanViewModel vm;
    @Nullable private ImageCapture imageCapture;
    private boolean cameraStarting;
    private boolean capturing;
    private ExecutorService captureExecutor;
    private Executor mainExecutor;

    // 进入本页时改为深色系统栏与黑色底，离开时恢复
    private boolean barsApplied;
    private boolean savedLightStatus;
    private boolean savedLightNav;
    @Nullable private View barsContainer;
    @Nullable private Drawable savedContainerBg;

    private final ActivityResultLauncher<String> requestCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (vm == null) return;
                vm.setCameraDenied(!granted);
                if (granted) startCamera();
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onPicked);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        captureExecutor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScanBinding.inflate(inflater.cloneInContext(darkContext(requireContext())),
                container, false);
        return binding.getRoot();
    }

    /** 相机页始终为深色：以夜间配置解析主题颜色（与原型的深色相机界面一致） */
    private static Context darkContext(Context base) {
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
        ContextThemeWrapper dark = new ContextThemeWrapper(base, R.style.Theme_HeBan);
        dark.applyOverrideConfiguration(cfg);
        return dark;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(nav().getBackStackEntry(R.id.scan_graph), HeBanApp.factory(requireContext()))
                .get(ScanViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        mainExecutor = ContextCompat.getMainExecutor(requireContext());

        binding.btnClose.setOnClickListener(v -> nav().navigateUp());
        binding.btnShutter.setOnClickListener(v -> takePicture());
        binding.btnAlbum.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            pickImage.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
        binding.btnHistory.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag(ScanHistorySheet.TAG) == null) {
                new ScanHistorySheet().show(getChildFragmentManager(), ScanHistorySheet.TAG);
            }
        });
        binding.btnGrant.setOnClickListener(v -> grantCamera());

        vm.flashOn.observe(getViewLifecycleOwner(), on -> {
            if (imageCapture != null) imageCapture.setFlashMode(flashMode());
        });
        vm.openResult.observe(getViewLifecycleOwner(), this::openResult);
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));

        if (hasCameraPermission()) {
            vm.setCameraDenied(false);
            startCamera();
        } else if (vm.shouldRequestPermission()) {
            requestCamera.launch(Manifest.permission.CAMERA);
        } else {
            vm.setCameraDenied(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        applyDarkBars();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从系统设置授权后返回
        if (Boolean.TRUE.equals(vm.cameraDenied.getValue()) && hasCameraPermission()) {
            vm.setCameraDenied(false);
            startCamera();
        }
    }

    @Override
    public void onStop() {
        restoreBars();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageCapture = null;
        cameraStarting = false;
        capturing = false;
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        captureExecutor.shutdown();
    }

    // ———————————————— 相机 ————————————————

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** 「去授权」：还能弹系统对话框时再次申请，否则打开应用设置 */
    private void grantCamera() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            requestCamera.launch(Manifest.permission.CAMERA);
            return;
        }
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().getPackageName(), null));
        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            requestCamera.launch(Manifest.permission.CAMERA);
        }
    }

    /** 绑定预览与拍照（技术准备文档 11.3）；没有相机或启动失败时提示「相机启动失败」，页面照常显示 */
    private void startCamera() {
        if (binding == null || cameraStarting || imageCapture != null) return;
        cameraStarting = true;
        try {
            ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
            future.addListener(() -> {
                if (binding == null) return;
                try {
                    ProcessCameraProvider provider = future.get();
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
                    ImageCapture capture = new ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(flashMode())
                            .build();
                    provider.unbindAll();
                    provider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA,
                            preview, capture);
                    imageCapture = capture;
                } catch (Exception | LinkageError e) {
                    cameraFailed();
                } finally {
                    cameraStarting = false;
                }
            }, mainExecutor);
        } catch (Exception | LinkageError e) {
            cameraStarting = false;
            cameraFailed();
        }
    }

    private void cameraFailed() {
        imageCapture = null;
        if (binding != null) Snackbars.show(binding.getRoot(), getString(R.string.scan_camera_failed));
    }

    private int flashMode() {
        return Boolean.TRUE.equals(vm.flashOn.getValue()) ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF;
    }

    private void takePicture() {
        if (!LoginRequired.check(this)) return;
        if (Boolean.TRUE.equals(vm.cameraDenied.getValue())) {
            grantCamera();
            return;
        }
        ImageCapture capture = imageCapture;
        if (capture == null) {
            Snackbars.show(binding.getRoot(), getString(R.string.scan_camera_failed));
            if (hasCameraPermission()) startCamera();
            return;
        }
        if (capturing) return;
        capturing = true;
        capture.takePicture(captureExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap upright;
                try {
                    upright = ScanImages.upright(image);
                } catch (RuntimeException e) {
                    mainExecutor.execute(() -> captureFailed());
                    return;
                } finally {
                    image.close();
                }
                mainExecutor.execute(() -> {
                    capturing = false;
                    if (binding != null) vm.recognize(upright);    // 进入 D03 并开始识别
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException e) {
                mainExecutor.execute(() -> captureFailed());
            }
        });
    }

    private void captureFailed() {
        capturing = false;
        if (binding != null) Snackbars.show(binding.getRoot(), getString(R.string.scan_capture_failed));
    }

    private void onPicked(@Nullable Uri uri) {
        if (uri == null || vm == null) return;
        Context app = requireContext().getApplicationContext();
        vm.recognizeFrom(() -> ScanImages.decode(app, uri));
    }

    private void openResult(@Nullable Long recordId) {
        NavDestination cur = nav().getCurrentDestination();
        if (cur == null || cur.getId() != R.id.scanFragment) return;
        Bundle args = new Bundle();
        if (recordId != null && recordId > 0) args.putLong(NavArgs.SCAN_RECORD_ID, recordId);
        nav().navigate(R.id.scanResultFragment, args);
    }

    // ———————————————— 系统栏 ————————————————

    private void applyDarkBars() {
        if (barsApplied || getActivity() == null || getView() == null) return;
        Window window = requireActivity().getWindow();
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(window, window.getDecorView());
        savedLightStatus = c.isAppearanceLightStatusBars();
        savedLightNav = c.isAppearanceLightNavigationBars();
        c.setAppearanceLightStatusBars(false);
        c.setAppearanceLightNavigationBars(false);
        // 导航宿主为系统栏留出的内边距区域也显示黑色
        if (getView().getParent() instanceof View) {
            barsContainer = (View) getView().getParent();
            savedContainerBg = barsContainer.getBackground();
            barsContainer.setBackgroundColor(Color.BLACK);
        }
        barsApplied = true;
    }

    private void restoreBars() {
        if (!barsApplied || getActivity() == null) return;
        Window window = requireActivity().getWindow();
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(window, window.getDecorView());
        c.setAppearanceLightStatusBars(savedLightStatus);
        c.setAppearanceLightNavigationBars(savedLightNav);
        if (barsContainer != null) barsContainer.setBackground(savedContainerBg);
        barsContainer = null;
        savedContainerBg = null;
        barsApplied = false;
    }
}
