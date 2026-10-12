package com.heban.app.ui.discover;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.ui.discover.pest.PestsFormat;
import com.heban.app.util.ImagePaths;

import java.util.List;
import java.util.Objects;

/** D01「附近好物」商品卡片：封面用 Glide 加载（缺图露出 ph_image 占位）、价格 + 单位、卖家 · 距离。 */
public class DiscoverProductAdapter extends ListAdapter<ProductEntity, DiscoverProductAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull ProductEntity product);
    }

    private final Listener listener;
    private final Gson gson;

    public DiscoverProductAdapter(@NonNull Gson gson, @NonNull Listener listener) {
        super(DIFF);
        this.gson = gson;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.discover_item_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ProductEntity p = getItem(position);
        Context ctx = h.itemView.getContext();

        List<String> images = PestsFormat.jsonList(gson, p.images);
        Uri cover = images.isEmpty() ? null : ImagePaths.toUri(images.get(0));
        if (cover == null) {
            Glide.with(h.image).clear(h.image);
            h.image.setImageDrawable(null);
        } else {
            // 图片不存在或加载失败时保持透明，露出背景上的 ph_image 占位
            Drawable none = new ColorDrawable(Color.TRANSPARENT);
            Glide.with(h.image).load(cover).centerCrop().placeholder(none).error(none).into(h.image);
        }

        h.title.setText(p.title);
        h.price.setText(price(h.price, p));
        String distance = ctx.getString(R.string.discover_seller_distance,
                p.sellerName == null ? "" : p.sellerName, DiscoverFormat.distance(p.distanceKm));
        h.seller.setText(distance);
        h.itemView.setContentDescription(PestsFormat.join("，", p.title, h.price.getText().toString(), distance));
        h.itemView.setOnClickListener(v -> listener.onOpen(p));
    }

    /** 「¥25 /斤」：单位 12sp、On Surface Variant；没有单位时只显示价格 */
    private static CharSequence price(TextView view, ProductEntity p) {
        SpannableStringBuilder sb = new SpannableStringBuilder(DiscoverFormat.money(p.priceCent));
        if (p.unit != null && !p.unit.trim().isEmpty()) {
            int start = sb.length();
            sb.append(view.getContext().getString(R.string.discover_price_unit, p.unit.trim()));
            int color = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant);
            TextAppearanceSpan body = new TextAppearanceSpan(view.getContext(),
                    com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
            sb.setSpan(body, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new AbsoluteSizeSpan(12, true), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(color), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final TextView price;
        final TextView seller;

        Holder(View v) {
            super(v);
            image = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            price = v.findViewById(R.id.price);
            seller = v.findViewById(R.id.seller);
        }
    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF = new DiffUtil.ItemCallback<ProductEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity a, @NonNull ProductEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity a, @NonNull ProductEntity b) {
            return Objects.equals(a.title, b.title) && a.priceCent == b.priceCent
                    && Objects.equals(a.unit, b.unit) && Objects.equals(a.sellerName, b.sellerName)
                    && a.distanceKm == b.distanceKm && Objects.equals(a.images, b.images);
        }
    };
}
