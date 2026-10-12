package com.heban.app.ui.discover.pest;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;
import com.heban.app.ui.common.image.RoundedCornersTransformation;
import com.heban.app.util.ImagePaths;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * D05 顶部大图：Picasso fit().centerCrop() + 圆角变换（技术准备文档 10.2）。
 * 缺图或加载失败时保留占位（图片图标 + 图片说明）；加载成功后说明移到左下角。
 */
public class PestDetailImageAdapter extends ListAdapter<PestDetailImage, PestDetailImageAdapter.Holder> {
    /** 没有图片说明时的无障碍描述用（「黄瓜霜霉病 图片 1」） */
    private String pestName = "";

    public PestDetailImageAdapter() {
        super(DIFF);
    }

    public void setPestName(@NonNull String name) {
        pestName = name;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pest_detail_item_image, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        PestDetailImage item = getItem(position);
        String caption = item.caption == null ? "" : item.caption;
        h.caption.setText(caption);
        h.caption.setVisibility(caption.isEmpty() ? View.GONE : View.VISIBLE);
        h.overlay.setText(caption);
        showPlaceholder(h, true);
        h.itemView.setContentDescription(caption.isEmpty()
                ? h.itemView.getContext().getString(R.string.pest_detail_image_cd, pestName, position + 1)
                : caption);

        Uri uri = ImagePaths.toUri(item.path);
        if (h.requested) {
            Picasso.get().cancelRequest(h.photo);
            h.requested = false;
        }
        h.photo.setImageDrawable(null);
        if (uri == null) return;
        float radius = h.itemView.getResources().getDimension(R.dimen.corner_large);
        h.requested = true;
        Picasso.get().load(uri)
                .noPlaceholder()
                .fit().centerCrop()
                .transform(new RoundedCornersTransformation(radius))
                .into(h.photo, new Callback() {
                    @Override
                    public void onSuccess() {
                        showPlaceholder(h, false);
                    }

                    @Override
                    public void onError(Exception e) {
                        h.photo.setImageDrawable(null);
                        showPlaceholder(h, true);
                    }
                });
    }

    private static void showPlaceholder(Holder h, boolean show) {
        h.placeholder.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        boolean hasCaption = h.overlay.getText() != null && h.overlay.getText().length() > 0;
        h.overlay.setVisibility(!show && hasCaption ? View.VISIBLE : View.GONE);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View placeholder;
        final TextView caption;
        final ImageView photo;
        final TextView overlay;
        /** 是否向 Picasso 发起过请求（复用时先取消） */
        boolean requested;

        Holder(View v) {
            super(v);
            placeholder = v.findViewById(R.id.placeholder);
            caption = v.findViewById(R.id.caption);
            photo = v.findViewById(R.id.photo);
            overlay = v.findViewById(R.id.caption_overlay);
        }
    }

    private static final DiffUtil.ItemCallback<PestDetailImage> DIFF = new DiffUtil.ItemCallback<PestDetailImage>() {
        @Override
        public boolean areItemsTheSame(@NonNull PestDetailImage a, @NonNull PestDetailImage b) {
            return a.equals(b);
        }

        @Override
        public boolean areContentsTheSame(@NonNull PestDetailImage a, @NonNull PestDetailImage b) {
            return a.equals(b);
        }
    };
}
