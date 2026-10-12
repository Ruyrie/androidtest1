package com.heban.app.ui.discover.market;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.paging.CombinedLoadStates;
import androidx.paging.LoadState;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.MarketRepository;
import com.heban.app.databinding.FragmentMarketBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/** D07 农产品集市（原型 Market.dc.html）。 */
public class MarketFragment extends BaseFragment {
    private static final String[] SORTS = {
            MarketRepository.SORT_DISTANCE, MarketRepository.SORT_NEWEST, MarketRepository.SORT_PRICE};

    private FragmentMarketBinding binding;
    private MarketViewModel vm;
    private MarketAdapter adapter;
    private Function1<CombinedLoadStates, Unit> loadListener;
    private Function0<Unit> pagesListener;
    /** 最近一次的加载状态（null 表示还没有回调） */
    @Nullable private CombinedLoadStates loadStates;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMarketBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(MarketViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItem);
        binding.fabPublish.setOnClickListener(v -> publish());
        binding.btnSort.setOnClickListener(this::showSortMenu);

        setUpList();
        setUpChips();
        setUpSearch();

        vm.count.observe(getViewLifecycleOwner(), n -> renderCount());
        vm.district.observe(getViewLifecycleOwner(), d -> renderCount());
        vm.favoritesOnlyOn.observe(getViewLifecycleOwner(), this::renderFavorites);
        vm.toast.observe(getViewLifecycleOwner(), msg -> {
            if (binding != null && msg != null) Snackbars.show(binding.getRoot(), msg);
        });
    }

    @Override
    public void onDestroyView() {
        if (adapter != null && loadListener != null) adapter.removeLoadStateListener(loadListener);
        if (adapter != null && pagesListener != null) adapter.removeOnPagesUpdatedListener(pagesListener);
        super.onDestroyView();
        binding.list.setAdapter(null);
        binding = null;
        adapter = null;
        loadListener = null;
        pagesListener = null;
        loadStates = null;
    }

    private void publish() {
        if (!LoginRequired.check(this)) return;
        nav().navigate(R.id.addProductFragment);
    }

    // ---------- 网格 ----------

    private void setUpList() {
        adapter = new MarketAdapter(container().gson, this::openProduct);
        binding.list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        int half = getResources().getDimensionPixelSize(R.dimen.card_gap) / 2;
        int gap = getResources().getDimensionPixelSize(R.dimen.card_gap);
        binding.list.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect out, @NonNull View child, @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                out.set(half, 0, half, gap);
            }
        });
        binding.list.setAdapter(adapter);

        // 技术准备文档 7.6：BaseFragment 的 disposables 在 onDestroyView 时清除
        disposables.add(vm.products
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    if (adapter != null) adapter.submitData(getViewLifecycleOwner().getLifecycle(), data);
                }, e -> {
                    if (binding != null) Snackbars.show(binding.getRoot(), AppError.from(e).message);
                }));

        // 加载状态变化或列表更新（包括更新为空列表）后都刷新空状态
        loadListener = states -> {
            loadStates = states;
            renderEmpty();
            return Unit.INSTANCE;
        };
        pagesListener = () -> {
            renderEmpty();
            return Unit.INSTANCE;
        };
        adapter.addLoadStateListener(loadListener);
        adapter.addOnPagesUpdatedListener(pagesListener);
        binding.empty.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_storefront));
    }

    private void openProduct(ProductEntity p) {
        Bundle args = new Bundle();
        args.putLong(NavArgs.PRODUCT_ID, p.id);
        nav().navigate(R.id.productFragment, args);
    }

    /** 加载完成且没有任何商品时显示空状态（按分类 / 搜索 / 收藏区分文字） */
    private void renderEmpty() {
        if (binding == null || adapter == null) return;
        boolean loaded = loadStates != null && loadStates.getRefresh() instanceof LoadState.NotLoading;
        boolean empty = loaded && adapter.getItemCount() == 0;
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);
        if (!empty) return;
        boolean searching = vm.hasKeyword();
        boolean favorites = Boolean.TRUE.equals(vm.favoritesOnlyOn.getValue());
        if (searching) {
            binding.empty.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_search));
            binding.empty.setTitle(getString(R.string.market_empty_search));
            binding.empty.setDesc(getString(R.string.market_empty_search_desc));
            binding.empty.setAction(null);
        } else if (favorites) {
            binding.empty.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite));
            binding.empty.setTitle(getString(R.string.market_empty_favorites));
            binding.empty.setDesc(getString(R.string.market_empty_favorites_desc));
            binding.empty.setAction(null);
        } else {
            String cat = vm.category.getValue();
            binding.empty.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_storefront));
            binding.empty.setTitle(cat == null || MarketViewModel.CAT_ALL.equals(cat)
                    ? getString(R.string.market_empty_all)
                    : getString(R.string.market_empty_category, cat));
            binding.empty.setDesc(getString(R.string.market_empty_desc));
            binding.empty.setAction(getString(R.string.market_empty_action));
            binding.empty.setOnAction(v -> publish());
        }
        binding.empty.executePendingBindings();
    }

    // ---------- 分类 / 数量 / 排序 ----------

    private void setUpChips() {
        vm.category.observe(getViewLifecycleOwner(), cat -> {
            int id = chipFor(cat);
            if (binding.chips.getCheckedChipId() != id) binding.chips.check(id);
        });
        binding.chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (!ids.isEmpty()) vm.selectCategory(categoryFor(ids));
        });
    }

    private static String categoryFor(List<Integer> ids) {
        int id = ids.get(0);
        if (id == R.id.chip_produce) return ProductEntity.CAT_PRODUCE;
        if (id == R.id.chip_supply) return ProductEntity.CAT_SUPPLY;
        if (id == R.id.chip_seedling) return ProductEntity.CAT_SEEDLING;
        if (id == R.id.chip_machine) return ProductEntity.CAT_MACHINE;
        return MarketViewModel.CAT_ALL;
    }

    private static int chipFor(String cat) {
        if (ProductEntity.CAT_PRODUCE.equals(cat)) return R.id.chip_produce;
        if (ProductEntity.CAT_SUPPLY.equals(cat)) return R.id.chip_supply;
        if (ProductEntity.CAT_SEEDLING.equals(cat)) return R.id.chip_seedling;
        if (ProductEntity.CAT_MACHINE.equals(cat)) return R.id.chip_machine;
        return R.id.chip_all;
    }

    private void renderCount() {
        Integer n = vm.count.getValue();
        int count = n == null ? 0 : n;
        String d = vm.district.getValue();
        binding.tvCount.setText(d == null
                ? getString(R.string.market_count_plain, count)
                : getString(R.string.market_count_region, d, count));
    }

    private void showSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int i = 0; i < SORTS.length; i++) menu.getMenu().add(0, i, i, sortLabel(SORTS[i]));
        menu.setOnMenuItemClickListener(item -> {
            vm.selectSort(SORTS[item.getItemId()]);
            binding.list.scrollToPosition(0);
            return true;
        });
        menu.show();
    }

    private String sortLabel(String sort) {
        if (MarketRepository.SORT_NEWEST.equals(sort)) return getString(R.string.market_sort_newest);
        if (MarketRepository.SORT_PRICE.equals(sort)) return getString(R.string.market_sort_price);
        return getString(R.string.market_sort_distance);
    }

    // ---------- 顶栏：搜索 / 收藏 ----------

    private boolean onMenuItem(MenuItem item) {
        if (item.getItemId() == R.id.market_action_search) {
            if (Boolean.TRUE.equals(vm.searchOpen.getValue())) {
                vm.closeSearch();
            } else {
                vm.openSearch();
            }
            return true;
        } else if (item.getItemId() == R.id.market_action_favorites) {
            vm.toggleFavoritesOnly();
            Snackbars.show(binding.getRoot(), getString(Boolean.TRUE.equals(vm.favoritesOnlyOn.getValue())
                    ? R.string.market_favorites_on : R.string.market_favorites_off));
            return true;
        }
        return false;
    }

    private void setUpSearch() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        vm.searchOpen.observe(getViewLifecycleOwner(), open -> {
            boolean on = Boolean.TRUE.equals(open);
            binding.toolbar.setTitle(on ? null : getString(R.string.market_title));
            binding.etSearch.setVisibility(on ? View.VISIBLE : View.GONE);
            MenuItem item = binding.toolbar.getMenu().findItem(R.id.market_action_search);
            item.setIcon(on ? R.drawable.ic_close : R.drawable.ic_search);
            item.setTitle(on ? R.string.market_search_close : R.string.market_search);
            if (on) {
                binding.etSearch.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
            } else {
                hideKeyboard();
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
        binding.etSearch.clearFocus();
    }

    /** 只看收藏时图标为实心 Error 色（与 D08 已收藏一致） */
    private void renderFavorites(Boolean on) {
        MenuItem item = binding.toolbar.getMenu().findItem(R.id.market_action_favorites);
        boolean active = Boolean.TRUE.equals(on);
        item.setIcon(active ? R.drawable.ic_favorite_fill : R.drawable.ic_favorite);
        item.setIconTintList(ColorStateList.valueOf(MarketStyles.color(binding.toolbar, active
                ? androidx.appcompat.R.attr.colorError
                : com.google.android.material.R.attr.colorOnSurfaceVariant)));
        item.setChecked(active);
    }
}
