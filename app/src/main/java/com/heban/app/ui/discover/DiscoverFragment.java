package com.heban.app.ui.discover;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.heban.app.R;
import com.heban.app.databinding.FragmentDiscoverBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.ui.discover.pest.PestsFragment;

/** D01 发现（原型 Discover.dc.html）。 */
public class DiscoverFragment extends BaseFragment {
    private FragmentDiscoverBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DiscoverViewModel vm = viewModel(DiscoverViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // 搜索栏 → D04 并聚焦搜索框；相机 / 拍照识别 → D02
        binding.searchBar.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean(PestsFragment.ARG_FOCUS_SEARCH, true);
            nav().navigate(R.id.pestsFragment, args);
        });
        binding.btnScan.setOnClickListener(v -> nav().navigate(R.id.scan_graph));
        binding.tileScan.setOnClickListener(v -> nav().navigate(R.id.scan_graph));
        binding.tilePests.setOnClickListener(v -> nav().navigate(R.id.pestsFragment));
        binding.tileMarket.setOnClickListener(v -> nav().navigate(R.id.marketFragment));
        binding.tileLedger.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            nav().navigate(R.id.ledgerFragment);
        });
        binding.btnGoMarket.setOnClickListener(v -> nav().navigate(R.id.marketFragment));

        int gap = getResources().getDimensionPixelSize(R.dimen.chip_gap);
        DiscoverSeasonAdapter seasonAdapter = new DiscoverSeasonAdapter(p -> {
            Bundle args = new Bundle();
            args.putLong(NavArgs.PEST_ID, p.id);
            nav().navigate(R.id.pestDetailFragment, args);
        });
        binding.seasonList.setAdapter(seasonAdapter);
        binding.seasonList.addItemDecoration(new DiscoverSpacing(gap));
        vm.seasonal.observe(getViewLifecycleOwner(), seasonAdapter::submitList);

        DiscoverProductAdapter productAdapter = new DiscoverProductAdapter(container().gson, p -> {
            Bundle args = new Bundle();
            args.putLong(NavArgs.PRODUCT_ID, p.id);
            nav().navigate(R.id.productFragment, args);
        });
        binding.nearbyList.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.nearbyList.setAdapter(productAdapter);
        binding.nearbyList.addItemDecoration(
                new DiscoverSpacing(getResources().getDimensionPixelSize(R.dimen.card_gap)));
        vm.nearby.observe(getViewLifecycleOwner(), productAdapter::submitList);

        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
