package com.heban.app.ui.crop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.heban.app.R;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.PlotCard;
import com.heban.app.databinding.FragmentPlotsBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

/** C05 地块管理（原型 Plots.dc.html）。添加 / 编辑地块用底部弹窗 {@link PlotEditSheet}。 */
public class PlotsFragment extends BaseFragment implements PlotsAdapter.Listener {
    private static final int MENU_EDIT = 1;
    private static final int MENU_READING = 2;
    private static final int MENU_DELETE = 3;

    private FragmentPlotsBinding binding;
    private PlotsViewModel vm;
    private PlotsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlotsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(PlotsViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.plots_action_add) {
                openEditor(0);
                return true;
            }
            return false;
        });
        binding.btnAddPlot.setOnClickListener(v -> openEditor(0));
        binding.empty.setOnAction(v -> openEditor(0));

        adapter = new PlotsAdapter(this);
        binding.list.setAdapter(adapter);
        vm.plots.observe(getViewLifecycleOwner(), adapter::submitList);
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
        vm.toastRes.observe(getViewLifecycleOwner(), res -> Snackbars.show(binding.getRoot(), getString(res)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }

    private void openEditor(long plotId) {
        if (!LoginRequired.check(this)) return;
        if (getChildFragmentManager().findFragmentByTag(PlotEditSheet.TAG) != null) return;
        PlotEditSheet.newInstance(plotId).show(getChildFragmentManager(), PlotEditSheet.TAG);
    }

    // ---------- PlotsAdapter.Listener ----------

    @Override
    public void onPlotClick(PlotCard card) {
        openEditor(card.plot.id);
    }

    @Override
    public void onMoreClick(View anchor, PlotCard card) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, MENU_EDIT, 0, R.string.plots_menu_edit);
        if (PlotsAdapter.isGreenhouse(card.plot)) menu.getMenu().add(0, MENU_READING, 1, R.string.plots_menu_reading);
        menu.getMenu().add(0, MENU_DELETE, 2, R.string.plots_menu_delete);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == MENU_EDIT) openEditor(card.plot.id);
            else if (id == MENU_READING) showReadingDialog(card);
            else if (id == MENU_DELETE) confirmDelete(card);
            return true;
        });
        menu.show();
    }

    @Override
    public void onCropClick(CropCard crop) {
        Bundle args = new Bundle();
        args.putLong(NavArgs.CROP_ID, crop.id);
        nav().navigate(R.id.cropDetailFragment, args);
    }

    @Override
    public void onReadingClick(PlotCard card) {
        showReadingDialog(card);
    }

    // ---------- 删除 / 记录温湿度 ----------

    private void confirmDelete(PlotCard card) {
        if (!LoginRequired.check(this)) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.plots_delete_title)
                .setMessage(getString(R.string.plots_delete_message, card.plot.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> vm.deletePlot(card.plot.id))
                .show();
    }

    private void showReadingDialog(PlotCard card) {
        if (!LoginRequired.check(this)) return;
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.plots_dialog_reading, null, false);
        TextInputLayout tilTemp = content.findViewById(R.id.til_temp);
        TextInputLayout tilHumidity = content.findViewById(R.id.til_humidity);
        TextInputEditText etTemp = content.findViewById(R.id.et_temp);
        TextInputEditText etHumidity = content.findViewById(R.id.et_humidity);
        PlotEntity p = card.plot;
        if (p.indoorTemp != null) etTemp.setText(CropsFormat.temperature(p.indoorTemp));
        if (p.indoorHumidity != null) etHumidity.setText(String.valueOf(p.indoorHumidity));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.plots_reading_title)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = etTemp.getText() == null ? "" : etTemp.getText().toString();
            String h = etHumidity.getText() == null ? "" : etHumidity.getText().toString();
            int err = vm.recordReading(p.id, t, h);
            tilTemp.setError(err == R.string.plots_error_temp ? getString(err) : null);
            tilHumidity.setError(err == R.string.plots_error_humidity ? getString(err) : null);
            if (err == 0) dialog.dismiss();
        }));
        dialog.show();
    }
}
