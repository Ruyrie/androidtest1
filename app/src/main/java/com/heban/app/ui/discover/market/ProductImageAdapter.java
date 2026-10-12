package com.heban.app.ui.discover.market;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;
import com.heban.app.ui.common.image.WatermarkTransformation;
import com.heban.app.util.ImagePaths;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D08 商品图片轮播（RecyclerView + PagerSnapHelper，技术准备文档 12.4）：
 * 详情大图用 Picasso + WatermarkTransformation（10.2 / 10.3）；没有图片时显示一页占位。
 */
public class ProductImageAdapter extends RecyclerView.Adapter<ProductImageAdapter.Holder> {
    private List<String> images = Collections.emptyList();
    @Nullable private String watermark;

    /** watermark 为「禾伴 @昵称」，为空时不加水印 */
    public void submit(@NonNull List<String> list, @Nullable String watermark) {
        this.images = new ArrayList<>(list);
        this.watermark = watermark;
        notifyDataSetChanged();
    }

    /** 真实图片数量（不含占位页） */
    public int imageCount() {
        return images.size();
    }

    @Override
    public int getItemCount() {
        return Math.max(1, images.size());
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView v = (ImageView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_item_image, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ImageView iv = holder.image;
        if (images.isEmpty()) {
            Picasso.get().cancelRequest(iv);
            iv.setImageDrawable(null);                       // 露出背景占位
            iv.setContentDescription(iv.getContext().getString(R.string.product_image_empty));
            return;
        }
        iv.setContentDescription(iv.getContext().getString(R.string.product_image_item, position + 1));
        RequestCreator req = Picasso.get()
                .load(ImagePaths.toUri(images.get(position)))
                .noPlaceholder()                                   // 占位图是 ImageView 背景
                .fit()
                .centerCrop();
        if (watermark != null && !watermark.isEmpty()) {
            float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12,
                    iv.getResources().getDisplayMetrics());
            // 右下角，位于页码「1 / 4」上方（页码距底 16dp、高 24dp）
            float end = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    iv.getResources().getDisplayMetrics());
            float bottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    iv.getResources().getDisplayMetrics());
            req = req.transform(new WatermarkTransformation(watermark, size, end, bottom));
        }
        req.into(iv);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;

        Holder(ImageView image) {
            super(image);
            this.image = image;
        }
    }
}
