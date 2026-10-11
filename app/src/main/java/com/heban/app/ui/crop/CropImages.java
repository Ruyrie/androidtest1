package com.heban.app.ui.crop;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.heban.app.R;
import com.heban.app.util.ImagePaths;

/**
 * 作物缩略图 / 封面：图标占位放在下层（Surface Container Highest 底 + 作物图标），
 * 封面图用 Glide centerCrop 盖在上层；没有封面或加载失败时上层保持透明，露出图标占位（原型占位样式）。
 */
public final class CropImages {
    private CropImages() {
    }

    /** 作物图标（CropCard.icon / CropProfile 图标），0 时用通用盆栽图标 */
    @BindingAdapter("cropIcon")
    public static void setIcon(ImageView view, @DrawableRes int icon) {
        view.setImageResource(icon != 0 ? icon : R.drawable.ic_potted_plant);
    }

    /** 封面图：ImagePaths.toUri(path)，无占位、失败不显示 */
    @BindingAdapter("cropCover")
    public static void setCover(ImageView view, @Nullable String path) {
        Uri uri = ImagePaths.toUri(path);
        if (uri == null) {
            Glide.with(view).clear(view);
            view.setImageDrawable(null);
            return;
        }
        Glide.with(view)
                .load(uri)
                .placeholder((Drawable) null)
                .error((Drawable) null)
                .centerCrop()
                .into(view);
    }
}
