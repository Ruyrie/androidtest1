package com.heban.app.ui.crop;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.heban.app.R;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropProfile;
import com.heban.app.databinding.FragmentAddCropBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * C03 添加 / 编辑作物（原型 AddCrop.dc.html）。参数：{@link NavArgs#CROP_ID}（> 0 为编辑），
 * {@link NavArgs#PLOT_ID}（预选地块）。游客打开时提示登录；保存后返回上一页并显示 Snackbar。
 */
public class AddCropFragment extends BaseFragment {
    private static final String TAG_DATE = "add_crop_date";
    private static final String STATE_CAMERA_TARGET = "camera_target";
    private static final String[] MODES = {CropEntity.MODE_GREENHOUSE, CropEntity.MODE_OPEN, CropEntity.MODE_POT};

    private FragmentAddCropBinding binding;
    private AddCropViewModel vm;
    private MaterialButton[] modeButtons;
    private AddCropSuggestionAdapter nameAdapter;
    private List<PlotEntity> plotList = Collections.emptyList();
    /** 相机写入的临时位置（拍照返回前 Fragment 可能被重建，需保存） */
    @Nullable
    private Uri cameraTarget;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickCover =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) viewModel().useCoverPhoto(uri, false);
            });

    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
                Uri target = cameraTarget;
                cameraTarget = null;
                if (target == null) return;
                if (Boolean.TRUE.equals(ok)) viewModel().useCoverPhoto(target, true);
                else new PhotoPickerFileStore(requireContext()).discardCameraTarget(target);
            });

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) launchCamera();
                else showMessage(getString(R.string.add_crop_camera_denied));
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddCropBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            cameraTarget = BundleCompat.getParcelable(savedInstanceState, STATE_CAMERA_TARGET, Uri.class);
        }
        vm = viewModel();
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        Bundle args = getArguments();
        long cropId = args == null ? 0 : args.getLong(NavArgs.CROP_ID, 0);
        long plotId = args == null ? 0 : args.getLong(NavArgs.PLOT_ID, 0);
        vm.start(cropId, plotId);

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.btnSave.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            AddCropFormUi.hideKeyboard(binding.getRoot());
            vm.save();
        });
        binding.cardCover.setOnClickListener(v -> showCoverOptions());

        setUpName();
        setUpPlot();
        setUpMode();
        setUpPlantedDate();
        setUpRemind();
        observeErrors();

        vm.messageRes.observe(getViewLifecycleOwner(), res -> showMessage(getString(res)));
        vm.toast.observe(getViewLifecycleOwner(), this::showMessage);
        vm.closePage.observe(getViewLifecycleOwner(), close -> nav().navigateUp());
        vm.saved.observe(getViewLifecycleOwner(), isNew -> AddCropFormUi.finishWithMessage(requireActivity(), nav(),
                getString(Boolean.TRUE.equals(isNew) ? R.string.add_crop_saved : R.string.add_crop_updated)));

        // 游客只能浏览：打开即提示登录
        if (savedInstanceState == null && !container().currentUser.isSignedIn()) LoginRequired.check(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraTarget != null) outState.putParcelable(STATE_CAMERA_TARGET, cameraTarget);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        modeButtons = null;
        nameAdapter = null;
        binding = null;
    }

    private AddCropViewModel viewModel() {
        if (vm == null) vm = viewModel(AddCropViewModel.class);
        return vm;
    }

    private void showMessage(@Nullable String text) {
        if (binding != null && text != null) Snackbars.show(binding.getRoot(), text);
    }

    // ---------- 作物照片 ----------

    private void showCoverOptions() {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.add_crop_photo_take));
        items.add(getString(R.string.add_crop_photo_pick));
        boolean hasCover = vm.hasCover();
        if (hasCover) items.add(getString(R.string.add_crop_photo_remove));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(hasCover ? R.string.add_crop_photo_change : R.string.add_crop_photo_title)
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    if (which == 0) onTakePhoto();
                    else if (which == 1) pickCover.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
                    else vm.removeCover();
                })
                .show();
    }

    private void onTakePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        PhotoPickerFileStore store = new PhotoPickerFileStore(requireContext());
        Uri target = store.createCameraTarget();
        if (target == null) {
            showMessage(getString(R.string.add_crop_camera_unavailable));
            return;
        }
        cameraTarget = target;
        try {
            takePhoto.launch(target);
        } catch (ActivityNotFoundException e) {
            cameraTarget = null;
            store.discardCameraTarget(target);
            showMessage(getString(R.string.add_crop_camera_unavailable));
        }
    }

    // ---------- 作物名称（生长期表建议，可自由输入） ----------

    private void setUpName() {
        nameAdapter = new AddCropSuggestionAdapter(requireContext());
        binding.etName.setAdapter(nameAdapter);
        vm.profiles.observe(getViewLifecycleOwner(), list -> {
            List<AddCropSuggestionAdapter.Item> items = new ArrayList<>();
            if (list != null) {
                for (CropProfile p : list) {
                    if (p == null || p.name == null || p.name.isEmpty()) continue;
                    items.add(new AddCropSuggestionAdapter.Item(p.name, AddCropFormUi.cropIcon(requireContext(), p.icon)));
                }
            }
            nameAdapter.setItems(items);
        });
    }

    // ---------- 种植地块 ----------

    private void setUpPlot() {
        vm.plots.observe(getViewLifecycleOwner(), list -> {
            plotList = list == null ? Collections.emptyList() : list;
            String[] names = new String[plotList.size() + 1];
            names[0] = getString(R.string.add_crop_plot_none);
            for (int i = 0; i < plotList.size(); i++) names[i + 1] = plotList.get(i).name;
            binding.etPlot.setSimpleItems(names);
            renderPlotName(vm.plotName.getValue());
        });
        vm.plotName.observe(getViewLifecycleOwner(), this::renderPlotName);
        binding.etPlot.setOnItemClickListener((parent, v, position, id) -> {
            if (position <= 0 || position > plotList.size()) vm.selectPlot(null);
            else vm.selectPlot(plotList.get(position - 1).id);
        });
    }

    private void renderPlotName(@Nullable String name) {
        binding.etPlot.setText(name != null ? name : getString(R.string.add_crop_plot_none), false);
    }

    // ---------- 种植方式 ----------

    private void setUpMode() {
        modeButtons = new MaterialButton[]{binding.btnModeGreenhouse, binding.btnModeOpen, binding.btnModePot};
        for (int i = 0; i < MODES.length; i++) modeButtons[i].setText(MODES[i]);
        vm.plantMode.observe(getViewLifecycleOwner(), mode -> {
            for (int i = 0; i < MODES.length; i++) {
                if (MODES[i].equals(mode) && !modeButtons[i].isChecked()) {
                    binding.groupMode.check(modeButtons[i].getId());
                }
            }
            renderModeIcons();
        });
        binding.groupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || modeButtons == null) return;
            for (int i = 0; i < modeButtons.length; i++) {
                if (modeButtons[i].getId() == checkedId) vm.selectMode(MODES[i]);
            }
            renderModeIcons();
        });
    }

    /** 选中项文字前显示勾（原型分段按钮选中态） */
    private void renderModeIcons() {
        for (MaterialButton b : modeButtons) b.setIconResource(b.isChecked() ? R.drawable.ic_check : 0);
    }

    // ---------- 定植日期 + 预计采收 ----------

    private void setUpPlantedDate() {
        View.OnClickListener open = v -> showDatePicker();
        binding.etPlanted.setOnClickListener(open);
        binding.tilPlanted.setEndIconOnClickListener(open);
        vm.plantedDate.observe(getViewLifecycleOwner(), d -> {
            binding.etPlanted.setText(d == null ? "" : AddCropFormUi.date(requireContext(), d));
            renderHarvest();
        });
        vm.expectedDays.observe(getViewLifecycleOwner(), days -> renderHarvest());
        reattachDatePicker();
    }

    /** 预计采收：2027年3月上旬（按 150 天计）；作物名称为空时不显示 */
    private void renderHarvest() {
        Integer days = vm.expectedDays.getValue();
        LocalDate harvest = AddCropHarvest.expectedHarvest(vm.plantedDate.getValue(), days == null ? 0 : days);
        if (harvest == null) {
            binding.tilPlanted.setHelperText(null);
            return;
        }
        String month = getString(R.string.add_crop_harvest_month, harvest.getYear(), harvest.getMonthValue(),
                getString(AddCropHarvest.periodRes(harvest)));
        binding.tilPlanted.setHelperText(getString(R.string.add_crop_harvest, month, days));
    }

    private void showDatePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_DATE) != null) return;
        LocalDate current = vm.plantedDate.getValue() == null ? LocalDate.now() : vm.plantedDate.getValue();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.add_crop_planted_pick)
                .setSelection(AddCropHarvest.toPickerSelection(current))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                        .setValidator(DateValidatorPointBackward.now())
                        .build())
                .build();
        attachDateListener(picker);
        picker.show(getChildFragmentManager(), TAG_DATE);
    }

    /** 旋转屏幕后对话框由系统恢复，重新挂上回调 */
    @SuppressWarnings("unchecked")
    private void reattachDatePicker() {
        Fragment f = getChildFragmentManager().findFragmentByTag(TAG_DATE);
        if (f instanceof MaterialDatePicker) attachDateListener((MaterialDatePicker<Long>) f);
    }

    private void attachDateListener(MaterialDatePicker<Long> picker) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel != null) vm.setPlantedDate(AddCropHarvest.fromPickerSelection(sel));
        });
    }

    // ---------- 开启农事提醒 ----------

    private void setUpRemind() {
        binding.rowRemind.setOnClickListener(v -> binding.swRemind.toggle());
        vm.remind.observe(getViewLifecycleOwner(),
                on -> AddCropFormUi.renderSwitchIcon(binding.swRemind, Boolean.TRUE.equals(on)));
    }

    // ---------- 校验错误 ----------

    private void observeErrors() {
        vm.nameError.observe(getViewLifecycleOwner(), res -> AddCropFormUi.renderError(binding.tilName,
                res == null || res == 0 ? null : getString(res)));
        vm.areaError.observe(getViewLifecycleOwner(), res -> {
            AddCropFormUi.renderError(binding.tilArea, res == null || res == 0 ? null : getString(res));
            // 错误消失后恢复说明文字
            if (res == null || res == 0) binding.tilArea.setHelperText(getString(R.string.add_crop_area_helper));
        });
    }
}
