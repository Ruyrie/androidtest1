package com.heban.app.ui.common;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.heban.app.R;
import com.heban.app.util.ImagePaths;

/** DataBinding 自定义属性（技术准备文档 6.2）。 */
public final class BindingAdapters {
    private BindingAdapters() {
    }

    /** 列表、头像等小图：Glide；没有图片时显示占位图 */
    @BindingAdapter(value = {"imageUrl", "placeholder"}, requireAll = false)
    public static void loadImage(ImageView view, String path, Drawable placeholder) {
        Drawable ph = placeholder != null ? placeholder : view.getContext().getDrawable(R.drawable.ph_image);
        Glide.with(view)
                .load(ImagePaths.toUri(path))
                .placeholder(ph)
                .error(ph)
                .centerCrop()
                .into(view);
    }

    @BindingAdapter("visible")
    public static void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
