package com.heban.app.ui.discover.market;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.heban.app.R;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.databinding.FragmentProductBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.util.ArrayList;
import java.util.List;

/**
 * D08 商品详情（原型 Product.dc.html）。参数：{@link NavArgs#PRODUCT_ID}。
 * 图片轮播 RecyclerView + PagerSnapHelper，Picasso + 水印；自己发布的商品底栏显示浏览数和删除。
 */
public class ProductFragment extends BaseFragment {
    /** S06 农友主页 / S09 私信的农友 id（目标页面的常量提供后改用其常量） */
    private static final String ARG_FARMER_ID = "farmer_id";

    private FragmentProductBinding binding;
    private ProductViewModel vm;
    private ProductImageAdapter images;
    private PagerSnapHelper snap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProductBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(ProductViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        Bundle args = getArguments();
        vm.start(args == null ? 0 : args.getLong(NavArgs.PRODUCT_ID, 0));

        binding.btnBack.setOnClickListener(v -> nav().navigateUp());
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnFav.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            vm.toggleFavorite();
        });
        binding.btnChat.setOnClickListener(v -> openChat());
        binding.btnContact.setOnClickListener(v -> dial());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnHome.setOnClickListener(v -> openSeller());

        setUpPager();
        vm.product.observe(getViewLifecycleOwner(), this::render);
        vm.images.observe(getViewLifecycleOwner(), list -> renderImages());
        vm.features.observe(getViewLifecycleOwner(), this::renderFeatures);
        vm.wants.observe(getViewLifecycleOwner(), n -> renderStats());
        vm.favorite.observe(getViewLifecycleOwner(), this::renderFavorite);
        vm.toast.observe(getViewLifecycleOwner(), this::showMessage);
        vm.deleted.observe(getViewLifecycleOwner(), x -> {
            View content = requireActivity().findViewById(android.R.id.content);
            nav().navigateUp();
            content.post(() -> Snackbars.show(content, getString(R.string.product_deleted)));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.pager.setAdapter(null);
        binding = null;
        images = null;
        snap = null;
    }

    private void showMessage(@Nullable String text) {
        if (binding != null && text != null) Snackbars.show(binding.getRoot(), text);
    }

    // ---------- 图片轮播 ----------

    private void setUpPager() {
        images = new ProductImageAdapter();
        binding.pager.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.pager.setAdapter(images);
        snap = new PagerSnapHelper();
        snap.attachToRecyclerView(binding.pager);
        binding.pager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int state) {
                if (state == RecyclerView.SCROLL_STATE_IDLE) renderIndicator();
            }
        });
    }

    private void renderImages() {
        ProductEntity p = vm.product.getValue();
        List<String> list = vm.images.getValue();
        String seller = p == null ? null : p.sellerName;
        images.submit(list == null ? new ArrayList<>() : list,
                seller == null || seller.trim().isEmpty() ? null : getString(R.string.product_watermark, seller.trim()));
        renderIndicator();
    }

    private void renderIndicator() {
        if (binding == null || images == null) return;
        int total = images.imageCount();
        binding.tvIndicator.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        if (total == 0) return;
        int pos = 0;
        View snapped = snap == null ? null : snap.findSnapView(binding.pager.getLayoutManager());
        if (snapped != null) pos = Math.max(0, binding.pager.getChildAdapterPosition(snapped));
        binding.tvIndicator.setText(getString(R.string.product_indicator, pos + 1, total));
    }

    // ---------- 内容 ----------

    private void render(@Nullable ProductEntity p) {
        if (p == null) return;
        binding.tvPrice.setText(getString(R.string.market_price, MarketFormat.price(p.priceCent)));
        binding.tvUnit.setText(unitText(p));
        renderStats();
        renderImages();

        // 卖家卡片：头像、昵称、认证、「句容白兔镇 · 种植 8 年 · 好评 98%」；自己的商品不显示「主页」
        binding.tvAvatar.setText(MarketFormat.avatarLetter(p.sellerName));
        MarketStyles.applyAvatar(binding.tvAvatar, MarketStyles.avatarSeed(p));
        binding.tvSeller.setText(p.sellerName);
        binding.ivVerified.setVisibility(p.sellerVerified ? View.VISIBLE : View.GONE);
        List<String> info = new ArrayList<>();
        if (p.sellerRegion != null && !p.sellerRegion.trim().isEmpty()) info.add(p.sellerRegion.trim());
        if (p.sellerYears > 0) info.add(getString(R.string.product_seller_years, p.sellerYears));
        if (p.sellerRating > 0) info.add(getString(R.string.product_seller_rating, p.sellerRating));
        binding.tvSellerInfo.setText(String.join(getString(R.string.product_sep), info));
        binding.tvSellerInfo.setVisibility(info.isEmpty() ? View.GONE : View.VISIBLE);
        binding.btnHome.setVisibility(!p.mine && p.sellerId != 0 ? View.VISIBLE : View.GONE);
        binding.sellerCard.setContentDescription(p.sellerName);

        boolean hasDesc = p.description != null && !p.description.trim().isEmpty();
        binding.sectionDesc.setVisibility(hasDesc ? View.VISIBLE : View.GONE);
        boolean any = row(binding.rowLocation, binding.tvLocation, p.location);
        any |= row(binding.rowSpec, binding.tvSpec, p.spec);
        any |= row(binding.rowStock, binding.tvStock, p.stockNote);
        binding.rows.setVisibility(any ? View.VISIBLE : View.GONE);

        // 底栏：他人的商品 收藏 / 私信 / 联系卖家（无电话时隐藏）；自己的商品 浏览数 + 删除
        boolean mine = p.mine;
        boolean hasPhone = p.sellerPhone != null && !p.sellerPhone.trim().isEmpty();
        binding.btnFav.setVisibility(mine ? View.GONE : View.VISIBLE);
        binding.btnChat.setVisibility(mine ? View.GONE : View.VISIBLE);
        binding.btnContact.setVisibility(!mine && hasPhone ? View.VISIBLE : View.GONE);
        binding.itemViews.setVisibility(mine ? View.VISIBLE : View.GONE);
        binding.btnDelete.setVisibility(mine ? View.VISIBLE : View.GONE);
        binding.tvViews.setText(getString(R.string.product_views, p.views));
        binding.itemViews.setContentDescription(getString(R.string.product_views, p.views));
    }

    private static boolean row(View row, android.widget.TextView value, @Nullable String text) {
        boolean has = text != null && !text.trim().isEmpty();
        row.setVisibility(has ? View.VISIBLE : View.GONE);
        value.setText(has ? text.trim() : null);
        return has;
    }

    /** 「/斤 · 10 斤起」；没有单位时只显示起售量 */
    private String unitText(ProductEntity p) {
        String unit = p.unit == null ? "" : p.unit.trim();
        List<String> parts = new ArrayList<>();
        if (!unit.isEmpty()) parts.add(getString(R.string.product_per_unit, unit));
        if (p.minQty > 0) parts.add(getString(R.string.product_min_qty, MarketFormat.number(p.minQty), unit).trim());
        return String.join(getString(R.string.product_sep), parts);
    }

    private void renderStats() {
        ProductEntity p = vm.product.getValue();
        Integer w = vm.wants.getValue();
        binding.tvStats.setText(getString(R.string.product_stats, w == null ? 0 : w, p == null ? 0 : p.views));
        binding.tvStats.setVisibility(p == null ? View.INVISIBLE : View.VISIBLE);
    }

    private void renderFeatures(@Nullable List<String> list) {
        binding.features.removeAllViews();
        if (list == null || list.isEmpty()) {
            binding.features.setVisibility(View.GONE);
            return;
        }
        binding.features.setVisibility(View.VISIBLE);
        for (String f : list) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.product_item_feature, binding.features, false);
            chip.setText(f);
            chip.setChipIconResource(featureIcon(f));
            binding.features.addView(chip);
        }
    }

    /** 特征标签图标（原型：verified 产地直供、storefront 可自提、local_shipping 同城配送、history_edu 附种植记录） */
    @DrawableRes
    private int featureIcon(String feature) {
        if (getString(R.string.product_feature_origin).equals(feature)) return R.drawable.ic_verified;
        if (getString(R.string.product_feature_pickup).equals(feature)) return R.drawable.ic_storefront;
        if (getString(R.string.product_feature_local).equals(feature)) return R.drawable.ic_local_shipping;
        if (getString(R.string.product_feature_express).equals(feature)) return R.drawable.ic_inventory_2;
        if (getString(R.string.product_feature_record).equals(feature)) return R.drawable.ic_history_edu;
        return R.drawable.ic_sell;
    }

    /** 收藏：未收藏 On Surface Variant 描边图标；已收藏 Error 色实心图标（原型 #BA1A1A） */
    private void renderFavorite(Boolean fav) {
        boolean on = Boolean.TRUE.equals(fav);
        binding.ivFav.setImageResource(on ? R.drawable.ic_favorite_fill : R.drawable.ic_favorite);
        binding.ivFav.setImageTintList(ColorStateList.valueOf(MarketStyles.color(binding.ivFav, on
                ? androidx.appcompat.R.attr.colorError
                : com.google.android.material.R.attr.colorOnSurfaceVariant)));
    }

    // ---------- 操作 ----------

    private void share() {
        ProductEntity p = vm.product.getValue();
        if (p == null) return;
        String price = getString(R.string.market_price, MarketFormat.price(p.priceCent))
                + (p.unit == null || p.unit.trim().isEmpty() ? "" : getString(R.string.product_per_unit, p.unit.trim()));
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.product_share_text, p.title, price));
        try {
            startActivity(Intent.createChooser(send, getString(R.string.product_share)));
        } catch (ActivityNotFoundException e) {
            showMessage(getString(R.string.feature_not_open));
        }
    }

    private void openSeller() {
        ProductEntity p = vm.product.getValue();
        if (p == null || p.sellerId == 0) return;
        Bundle args = new Bundle();
        args.putLong(ARG_FARMER_ID, p.sellerId);
        nav().navigate(R.id.userProfileFragment, args);
    }

    private void openChat() {
        if (!LoginRequired.check(this)) return;
        ProductEntity p = vm.product.getValue();
        if (p == null) return;
        Bundle args = new Bundle();
        args.putLong(ARG_FARMER_ID, p.sellerId);
        args.putLong(NavArgs.PRODUCT_ID, p.id);
        nav().navigate(R.id.chatFragment, args);
    }

    private void dial() {
        ProductEntity p = vm.product.getValue();
        if (p == null || p.sellerPhone == null || p.sellerPhone.trim().isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p.sellerPhone.trim())));
        } catch (ActivityNotFoundException e) {
            showMessage(getString(R.string.feature_not_open));
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.product_delete_title)
                .setMessage(R.string.product_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> vm.delete())
                .show();
    }
}
