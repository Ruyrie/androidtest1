package com.heban.app.ui.crop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.navigation.NavDestination;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.heban.app.R;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.databinding.FragmentCropDetailBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.time.ZoneId;
import java.util.List;

/** C02 作物详情（原型 CropDetail.dc.html），参数 NavArgs.CROP_ID。 */
public class CropDetailFragment extends BaseFragment {
    private static final int MENU_DELETE = 1;

    private FragmentCropDetailBinding binding;
    private CropDetailViewModel vm;
    private CropDetailRecordsAdapter recordsAdapter;
    private CropDetailPhotosAdapter photosAdapter;
    private final ZoneId zone = ZoneId.systemDefault();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCropDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(CropDetailViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        long cropId = getArguments() == null ? 0 : getArguments().getLong(NavArgs.CROP_ID, 0);

        binding.btnBack.setOnClickListener(v -> nav().navigateUp());
        binding.btnShare.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            nav().navigate(R.id.publishFragment, cropArgs());
        });
        binding.btnEdit.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            nav().navigate(R.id.addCropFragment, cropArgs());
        });
        binding.btnMore.setOnClickListener(this::showMoreMenu);
        binding.btnAskAi.setOnClickListener(v -> nav().navigate(R.id.aiChatFragment, cropArgs()));
        binding.btnAddRecord.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            nav().navigate(R.id.addRecordFragment, cropArgs());
        });
        binding.btnDismissAttention.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            vm.dismissAttention();
        });

        setUpTabs();
        recordsAdapter = new CropDetailRecordsAdapter(this::confirmDeleteRecord);
        binding.records.setAdapter(recordsAdapter);
        photosAdapter = new CropDetailPhotosAdapter();
        binding.photos.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.photos.setAdapter(photosAdapter);

        vm.detail.observe(getViewLifecycleOwner(), this::render);
        vm.records.observe(getViewLifecycleOwner(), recordsAdapter::submitList);
        vm.photos.observe(getViewLifecycleOwner(), photosAdapter::submitList);
        vm.typeStats.observe(getViewLifecycleOwner(), this::renderTypeStats);
        vm.totalCost.observe(getViewLifecycleOwner(), cost -> binding.tvGrowthTotal.setText(
                getString(R.string.crop_detail_growth_total, CropsFormat.money(cost == null ? 0 : cost))));
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
        vm.toastRes.observe(getViewLifecycleOwner(), res -> Snackbars.show(binding.getRoot(), getString(res)));
        vm.close.observe(getViewLifecycleOwner(), c -> {
            NavDestination cur = nav().getCurrentDestination();
            if (cur != null && cur.getId() == R.id.cropDetailFragment) nav().navigateUp();
        });

        vm.start(cropId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        recordsAdapter = null;
        photosAdapter = null;
    }

    private Bundle cropArgs() {
        Bundle b = new Bundle();
        b.putLong(NavArgs.CROP_ID, vm.cropId());
        return b;
    }

    private void setUpTabs() {
        Integer t = vm.tab.getValue();
        TabLayout.Tab sel = binding.tabs.getTabAt(t == null ? 0 : t);
        if (sel != null) sel.select();
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                vm.selectTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void render(@Nullable CropDetail d) {
        if (d == null || d.crop == null) return;
        CropCard card = d.card;
        int icon = card != null ? card.icon : 0;
        String cover = card != null && card.coverPath != null ? card.coverPath : d.crop.coverPath;
        CropImages.setIcon(binding.heroIcon, icon);
        CropImages.setCover(binding.heroCover, cover);

        binding.tvName.setText(d.crop.name);
        String plotName = d.plotName != null ? d.plotName : card != null ? card.plotName : null;
        binding.tvSubtitle.setText(CropsFormat.detailSubtitle(d.crop.variety, plotName, d.crop.areaMu));

        int daysPlanted = card != null ? card.daysPlanted : 0;
        binding.chipPlanted.setText(getString(R.string.crop_detail_planted_on,
                CropsFormat.monthDay(d.crop.plantedAt, zone)));
        binding.chipDays.setText(getString(R.string.crop_detail_days_planted, daysPlanted));
        binding.chipHarvest.setText(getString(R.string.crop_detail_harvest_on,
                CropsFormat.monthDay(d.expectedHarvestAt, zone)));

        String note = d.crop.attentionNote;
        boolean attention = note != null && !note.trim().isEmpty();
        binding.attention.setVisibility(attention ? View.VISIBLE : View.GONE);
        binding.tvAttention.setText(attention ? note : null);

        binding.tvDaysLeft.setText(d.daysLeft > 0
                ? getString(R.string.crop_detail_days_left, d.daysLeft)
                : getString(R.string.crop_detail_harvest_due));
        binding.stepper.setStages(d.stages);

        int expected = card != null && card.expectedDays > 0 ? card.expectedDays : d.crop.expectedDays;
        int progress = card != null ? card.progress : 0;
        binding.tvGrowthDays.setText(getString(R.string.crop_detail_growth_days_value, daysPlanted));
        binding.tvGrowthExpected.setText(getString(R.string.crop_detail_growth_days_value, expected));
        binding.tvGrowthProgress.setText(getString(R.string.crop_detail_growth_percent, progress));
    }

    private void renderTypeStats(@Nullable List<CropDetailViewModel.TypeStat> stats) {
        binding.growthTypes.removeAllViews();
        if (stats == null) return;
        LayoutInflater inflater = LayoutInflater.from(binding.growthTypes.getContext());
        for (CropDetailViewModel.TypeStat s : stats) {
            View row = inflater.inflate(R.layout.crop_detail_item_type_stat, binding.growthTypes, false);
            ((ImageView) row.findViewById(R.id.stat_icon)).setImageResource(s.type.icon);
            ((TextView) row.findViewById(R.id.stat_label)).setText(s.type.label);
            String detail = CropsFormat.join(getString(R.string.crop_detail_growth_count, s.count),
                    getString(R.string.crop_detail_growth_last, CropsFormat.monthDay(s.lastAt, zone)));
            ((TextView) row.findViewById(R.id.stat_detail)).setText(detail);
            ((TextView) row.findViewById(R.id.stat_cost)).setText(s.costCent > 0 ? CropsFormat.money(s.costCent) : "");
            binding.growthTypes.addView(row);
        }
    }

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, MENU_DELETE, 0, R.string.crop_detail_delete_crop);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_DELETE) {
                confirmDeleteCrop();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmDeleteCrop() {
        if (!LoginRequired.check(this)) return;
        CropDetail d = vm.detail.getValue();
        String name = d != null && d.crop != null ? d.crop.name : "";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.crop_detail_delete_crop_title)
                .setMessage(getString(R.string.crop_detail_delete_crop_message, name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dlg, w) -> vm.deleteCrop())
                .show();
    }

    private void confirmDeleteRecord(FarmRecordEntity record) {
        if (!LoginRequired.check(this)) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.crop_detail_delete_record_title)
                .setMessage(R.string.crop_detail_delete_record_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dlg, w) -> vm.deleteRecord(record.id))
                .show();
    }
}
