package com.heban.app.ui.discover.market;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.databinding.MarketItemProductBinding;
import com.heban.app.util.ImagePaths;

import java.util.Objects;

/** D07 商品网格（PagingDataAdapter，技术准备文档 7.6）。图片用 Glide，缺图时显示 ph_image。 */
public class MarketAdapter extends PagingDataAdapter<ProductEntity, MarketAdapter.Holder> {

    public interface OnProductClick {
        void onClick(ProductEntity product);
    }

    private final OnProductClick onClick;
    private final Gson gson;

    public MarketAdapter(@NonNull Gson gson, @NonNull OnProductClick onClick) {
        super(DIFF);
        this.gson = gson;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MarketItemProductBinding b = MarketItemProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        Holder h = new Holder(b);
        b.card.setClipToOutline(true);     // 图片顶部跟随卡片圆角
        b.card.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            ProductEntity p = peek(pos);
            if (p != null) onClick.onClick(p);
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        bind(holder.binding, getItem(position), gson, System.currentTimeMillis());
    }

    static void bind(@NonNull MarketItemProductBinding b, ProductEntity p, @NonNull Gson gson, long now) {
        Context ctx = b.getRoot().getContext();
        if (p == null) {
            b.tvTitle.setText(null);
            b.tvPrice.setText(null);
            b.tvSeller.setText(null);
            b.tvDistance.setText(null);
            b.tvAvatar.setText(null);
            b.tvTag.setVisibility(View.GONE);
            Glide.with(b.ivCover).clear(b.ivCover);
            return;
        }
        Glide.with(b.ivCover)
                .load(ImagePaths.toUri(MarketFormat.cover(gson, p)))
                .placeholder((Drawable) null)      // 占位图是 ImageView 背景（ph_image，图标保持 36dp）
                .error((Drawable) null)
                .fallback((Drawable) null)
                .centerCrop()
                .into(b.ivCover);

        boolean pinned = MarketFormat.isPinned(p, now);
        String tag = pinned
                ? ctx.getString(R.string.market_tag_pinned, MarketFormat.pinnedHoursLeft(p.pinnedUntil, now))
                : p.tag;
        boolean hasTag = tag != null && !tag.trim().isEmpty();
        b.tvTag.setVisibility(hasTag ? View.VISIBLE : View.GONE);
        if (hasTag) {
            b.tvTag.setText(tag);
            MarketStyles.applyTag(b.tvTag, p.tag, pinned);
        }

        b.tvTitle.setText(p.title);
        b.tvPrice.setText(price(ctx, p, MarketStyles.color(b.tvPrice,
                com.google.android.material.R.attr.colorOnSurfaceVariant)));

        b.tvAvatar.setText(MarketFormat.avatarLetter(p.sellerName));
        MarketStyles.applyAvatar(b.tvAvatar, MarketStyles.avatarSeed(p));
        b.tvSeller.setText(p.sellerName);
        b.tvDistance.setText(p.mine
                ? ctx.getString(R.string.market_mine)
                : ctx.getString(R.string.market_distance, MarketFormat.number(p.distanceKm)));
        b.card.setContentDescription(p.title);
    }

    /** 「¥25」+「 /斤」（单位 12sp 常规字重 On Surface Variant） */
    static CharSequence price(@NonNull Context ctx, @NonNull ProductEntity p, int unitColor) {
        SpannableStringBuilder sb = new SpannableStringBuilder(
                ctx.getString(R.string.market_price, MarketFormat.price(p.priceCent)));
        if (p.unit != null && !p.unit.trim().isEmpty()) {
            int start = sb.length();
            sb.append(ctx.getString(R.string.market_unit, p.unit.trim()));
            int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12,
                    ctx.getResources().getDisplayMetrics()));
            sb.setSpan(new AbsoluteSizeSpan(px), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(unitColor), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new TypefaceSpan("sans-serif"), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MarketItemProductBinding binding;

        Holder(MarketItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final DiffUtil.ItemCallback<ProductEntity> DIFF = new DiffUtil.ItemCallback<ProductEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity a, @NonNull ProductEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity a, @NonNull ProductEntity b) {
            return Objects.equals(a.title, b.title) && a.priceCent == b.priceCent
                    && Objects.equals(a.unit, b.unit) && Objects.equals(a.tag, b.tag)
                    && a.pinnedUntil == b.pinnedUntil && Objects.equals(a.images, b.images)
                    && Objects.equals(a.sellerName, b.sellerName) && a.mine == b.mine
                    && Double.compare(a.distanceKm, b.distanceKm) == 0;
        }
    };
}
