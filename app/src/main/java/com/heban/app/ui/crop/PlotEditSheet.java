package com.heban.app.ui.crop;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.databinding.PlotsSheetEditBinding;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

/** C05 添加 / 编辑地块底部弹窗；参数 NavArgs.PLOT_ID（0 或缺省为新增）。 */
public class PlotEditSheet extends BottomSheetDialogFragment {
    public static final String TAG = "plot_edit";

    private PlotsSheetEditBinding binding;
    private PlotEditViewModel vm;

    public static PlotEditSheet newInstance(long plotId) {
        PlotEditSheet sheet = new PlotEditSheet();
        Bundle args = new Bundle();
        args.putLong(NavArgs.PLOT_ID, plotId);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_HeBan_Plots_BottomSheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = PlotsSheetEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this, HeBanApp.factory(requireContext())).get(PlotEditViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        long plotId = getArguments() == null ? 0 : getArguments().getLong(NavArgs.PLOT_ID, 0);

        vm.type.observe(getViewLifecycleOwner(), t -> {
            int id = PlotEntity.TYPE_OPEN.equals(t) ? R.id.chip_open : R.id.chip_greenhouse;
            if (binding.typeChips.getCheckedChipId() != id) binding.typeChips.check(id);
        });
        binding.typeChips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            vm.selectType(ids.get(0) == R.id.chip_open ? PlotEntity.TYPE_OPEN : PlotEntity.TYPE_GREENHOUSE);
        });
        vm.nameError.observe(getViewLifecycleOwner(), res ->
                binding.tilName.setError(res == null || res == 0 ? null : getString(res)));
        vm.areaError.observe(getViewLifecycleOwner(), res ->
                binding.tilArea.setError(res == null || res == 0 ? null : getString(res)));
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
        vm.saved.observe(getViewLifecycleOwner(), ok -> {
            if (getParentFragment() != null && getParentFragment().getView() != null) {
                Snackbars.show(getParentFragment().getView(), getString(R.string.plots_saved));
            }
            dismissAllowingStateLoss();
        });
        binding.btnCancel.setOnClickListener(v -> dismiss());

        vm.start(plotId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
