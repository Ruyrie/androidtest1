package com.heban.app.ui.discover.pest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;

import com.heban.app.R;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.databinding.FragmentPestsBinding;
import com.heban.app.databinding.PestsHeaderBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.ui.discover.DiscoverSpacing;

import java.util.List;
import java.util.Objects;

/**
 * D04 病虫害百科（原型 Pests.dc.html）。参数 {@link #ARG_FOCUS_SEARCH}（boolean，可选）：
 * 从 D01 搜索栏进入时自动聚焦搜索框并弹出键盘。
 */
public class PestsFragment extends BaseFragment {
    /** boolean：进入后聚焦搜索框（D01 搜索栏） */
    public static final String ARG_FOCUS_SEARCH = "focus_search";

    private FragmentPestsBinding binding;
    private PestsHeaderBinding header;
    private PestsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(PestsViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.btnBack.setOnClickListener(v -> nav().navigateUp());
        setUpSearch();

        PestsHeaderAdapter headerAdapter = new PestsHeaderAdapter(vm, getViewLifecycleOwner(), this::setUpHeader);
        PestsAdapter gridAdapter = new PestsAdapter(container().gson, this::openPest);
        ConcatAdapter adapter = new ConcatAdapter(headerAdapter, gridAdapter);
        GridLayoutManager glm = new GridLayoutManager(requireContext(), 2);
        // 第 0 项为列表头（ConcatAdapter 会重新编号 viewType，按位置判断）
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < headerAdapter.getItemCount() ? 2 : 1;
            }
        });
        binding.list.setLayoutManager(glm);
        binding.list.setAdapter(adapter);
        binding.list.addItemDecoration(new DiscoverSpacing(getResources().getDimensionPixelSize(R.dimen.card_gap)));
        vm.pests.observe(getViewLifecycleOwner(), gridAdapter::submitList);

        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));

        Bundle args = getArguments();
        if (vm.consumeFocusRequest(args != null && args.getBoolean(ARG_FOCUS_SEARCH, false))) {
            binding.etSearch.requestFocus();
            binding.etSearch.post(() -> {
                if (binding == null) return;
                InputMethodManager imm = requireContext().getSystemService(InputMethodManager.class);
                if (imm != null) imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        header = null;
    }

    private void openPest(@NonNull PestEntity p) {
        hideKeyboard();
        Bundle args = new Bundle();
        args.putLong(NavArgs.PEST_ID, p.id);
        nav().navigate(R.id.pestDetailFragment, args);
    }

    // ---------- 搜索 ----------

    private void setUpSearch() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        vm.search.observe(getViewLifecycleOwner(), s -> {
            boolean has = s != null && !s.isEmpty();
            binding.btnTrailing.setImageResource(has ? R.drawable.ic_close : R.drawable.ic_photo_camera);
            binding.btnTrailing.setContentDescription(getString(has ? R.string.pests_clear_search : R.string.discover_scan_cd));
        });
        binding.btnTrailing.setOnClickListener(v -> {
            String s = vm.search.getValue();
            if (s != null && !s.isEmpty()) {
                vm.clearSearch();
            } else {
                hideKeyboard();
                nav().navigate(R.id.scan_graph);
            }
        });
    }

    private void hideKeyboard() {
        if (binding == null) return;
        InputMethodManager imm = requireContext().getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
        binding.etSearch.clearFocus();
    }

    // ---------- 列表头：筛选 Chip、作物下拉、横幅 ----------

    private void setUpHeader(@NonNull PestsHeaderBinding b) {
        header = b;
        vm.selectedKind.observe(getViewLifecycleOwner(), k -> {
            int id = chipFor(k);
            if (b.chips.getCheckedChipId() != id) b.chips.check(id);
        });
        b.chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            vm.selectKind(kindFor(ids.get(0)));
        });
        b.chipCrop.setOnClickListener(this::showCropMenu);
        b.chipCrop.setOnCloseIconClickListener(this::showCropMenu);
        View.OnClickListener openBanner = v -> {
            PestEntity p = vm.banner.getValue();
            if (p != null) openPest(p);
        };
        b.banner.setOnClickListener(openBanner);
        b.btnBanner.setOnClickListener(openBanner);
    }

    /** 作物下拉：「全部作物」+ 百科中出现的作物 */
    private void showCropMenu(View anchor) {
        if (header == null) return;
        // 点击 Chip 会切换选中态，这里恢复为当前筛选状态
        header.chipCrop.setChecked(vm.selectedCrop.getValue() != null);
        PopupMenu menu = new PopupMenu(requireContext(), header.chipCrop);
        Menu m = menu.getMenu();
        m.add(Menu.NONE, 0, 0, R.string.pests_crop_all);
        List<String> options = vm.cropOptions.getValue();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) m.add(Menu.NONE, i + 1, i + 1, options.get(i));
        }
        String current = vm.selectedCrop.getValue();
        for (int i = 0; i < m.size(); i++) {
            m.getItem(i).setCheckable(true);
            m.getItem(i).setChecked(i == 0 ? current == null : Objects.equals(current, m.getItem(i).getTitle().toString()));
        }
        m.setGroupCheckable(Menu.NONE, true, true);
        menu.setOnMenuItemClickListener(item -> {
            vm.selectCrop(item.getItemId() == 0 ? null : item.getTitle().toString());
            return true;
        });
        menu.show();
    }

    private static String kindFor(int id) {
        if (id == R.id.chip_disease) return PestEntity.KIND_DISEASE;
        if (id == R.id.chip_insect) return PestEntity.KIND_INSECT;
        if (id == R.id.chip_physiological) return PestEntity.KIND_PHYSIOLOGICAL;
        return PestsViewModel.KIND_ALL;
    }

    private static int chipFor(String k) {
        if (PestEntity.KIND_DISEASE.equals(k)) return R.id.chip_disease;
        if (PestEntity.KIND_INSECT.equals(k)) return R.id.chip_insect;
        if (PestEntity.KIND_PHYSIOLOGICAL.equals(k)) return R.id.chip_physiological;
        return R.id.chip_all;
    }
}
