package com.heban.app.ui.account;

import android.Manifest;
import android.content.pm.PackageManager;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.heban.app.R;
import com.heban.app.databinding.FragmentProfileSetupBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.NavRoutes;
import com.heban.app.ui.common.Snackbars;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

/** A06 完善资料（第 2 步，共 2 步）：可跳过。与 A05 共享 {@link RegisterViewModel}。 */
public class ProfileSetupFragment extends BaseFragment {
    private FragmentProfileSetupBinding binding;
    private RegisterViewModel vm;
    private final List<Chip> cropChips = new ArrayList<>();
    private MaterialButton[] identityButtons;
    @Nullable
    private Disposable locating;

    /** 系统照片选择器；所选头像只在本次会话中显示（M1 尚未保存头像） */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null && vm != null) vm.avatarUri.setValue(uri.toString());
            });

    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (binding == null) return;
                if (Boolean.TRUE.equals(granted)) locate();
                else showLocateFailed();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = RegisterFragment.sharedViewModel(this);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.avatar.setOnClickListener(v -> pickAvatar.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        binding.tilRegion.setEndIconOnClickListener(v -> onLocateClicked());

        setUpIdentity();
        setUpCrops();

        vm.loading.observe(getViewLifecycleOwner(),
                loading -> RegisterFragment.showButtonLoading(binding.btnEnter, Boolean.TRUE.equals(loading)));
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
        vm.toastRes.observe(getViewLifecycleOwner(), res -> Snackbars.show(binding.getRoot(), getString(res)));
        vm.enterHome.observe(getViewLifecycleOwner(), go -> NavRoutes.enterHome(nav()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cropChips.clear();
        identityButtons = null;
        locating = null;
        binding = null;
    }

    // ---------- 种植身份（分段按钮，单选） ----------

    private void setUpIdentity() {
        identityButtons = new MaterialButton[]{binding.btnIdentity0, binding.btnIdentity1, binding.btnIdentity2};
        for (int i = 0; i < identityButtons.length; i++) {
            identityButtons[i].setText(RegisterViewModel.IDENTITIES.get(i));
        }
        vm.identity.observe(getViewLifecycleOwner(), name -> {
            int idx = RegisterViewModel.IDENTITIES.indexOf(name);
            if (idx >= 0 && binding.groupIdentity.getCheckedButtonId() != identityButtons[idx].getId()) {
                binding.groupIdentity.check(identityButtons[idx].getId());
            }
            renderIdentityIcons();
        });
        binding.groupIdentity.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < identityButtons.length; i++) {
                if (identityButtons[i].getId() == checkedId) vm.selectIdentity(RegisterViewModel.IDENTITIES.get(i));
            }
            renderIdentityIcons();
        });
    }

    /** 选中项文字前显示勾（原型 .seg 选中态） */
    private void renderIdentityIcons() {
        for (MaterialButton b : identityButtons) {
            b.setIconResource(b.isChecked() ? R.drawable.ic_check : 0);
        }
    }

    // ---------- 主要作物（Filter Chip，多选） ----------

    private void setUpCrops() {
        LayoutInflater inflater = LayoutInflater.from(binding.groupCrops.getContext());
        for (String name : RegisterViewModel.CROP_OPTIONS) {
            Chip chip = (Chip) inflater.inflate(R.layout.register_item_crop_chip, binding.groupCrops, false);
            chip.setId(View.generateViewId());
            chip.setText(name);
            chip.setChecked(vm.isCropSelected(name));
            chip.setOnCheckedChangeListener((button, checked) -> vm.setCropSelected(name, checked));
            binding.groupCrops.addView(chip);
            cropChips.add(chip);
        }
        vm.selectedCrops.observe(getViewLifecycleOwner(), this::syncCrops);
    }

    private void syncCrops(Set<String> selected) {
        for (Chip chip : cropChips) {
            boolean on = selected != null && selected.contains(chip.getText().toString());
            if (chip.isChecked() != on) chip.setChecked(on);
        }
    }

    // ---------- 所在地区：定位 ----------

    private void onLocateClicked() {
        if (locating != null && !locating.isDisposed()) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locate();
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private void locate() {
        locating = new RegisterRegionLocator(requireContext()).locate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(region -> vm.region.setValue(region), e -> showLocateFailed());
        disposables.add(locating);
    }

    private void showLocateFailed() {
        if (binding != null) Snackbars.show(binding.getRoot(), getString(R.string.register_locate_failed));
    }
}
