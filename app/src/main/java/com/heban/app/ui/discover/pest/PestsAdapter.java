package com.heban.app.ui.discover.pest;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.util.ImagePaths;

import java.util.List;
import java.util.Objects;

/** D04 病虫害网格卡片：封面（images 第一张）用 Glide 加载，缺图时露出 ph_image 占位。 */
public class PestsAdapter extends ListAdapter<PestEntity, PestsAdapter.Holder> {
    static final int VIEW_TYPE = R.layout.pests_item_card;

    public interface Listener {
        void onOpen(@NonNull PestEntity pest);
    }

    private final Gson gson;
    private final Listener listener;

    public PestsAdapter(@NonNull Gson gson, @NonNull Listener listener) {
        super(DIFF);
        this.gson = gson;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.pests_item_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        PestEntity p = getItem(position);
        List<String> images = PestsFormat.jsonList(gson, p.images);
        Uri cover = images.isEmpty() ? null : ImagePaths.toUri(images.get(0));
        if (cover == null) {
            Glide.with(h.image).clear(h.image);
            h.image.setImageDrawable(null);
        } else {
            Drawable none = new ColorDrawable(Color.TRANSPARENT);
            Glide.with(h.image).load(cover).centerCrop().placeholder(none).error(none).into(h.image);
        }
        h.name.setText(p.name);
        String sub = PestsFormat.cardSubtitle(p);
        h.subtitle.setText(sub);
        h.subtitle.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);
        h.itemView.setContentDescription(PestsFormat.join("，", p.name, sub));
        h.itemView.setOnClickListener(v -> listener.onOpen(p));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView subtitle;

        Holder(View v) {
            super(v);
            image = v.findViewById(R.id.image);
            name = v.findViewById(R.id.name);
            subtitle = v.findViewById(R.id.subtitle);
        }
    }

    private static final DiffUtil.ItemCallback<PestEntity> DIFF = new DiffUtil.ItemCallback<PestEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull PestEntity a, @NonNull PestEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PestEntity a, @NonNull PestEntity b) {
            return Objects.equals(a.name, b.name) && Objects.equals(a.cropTags, b.cropTags)
                    && Objects.equals(a.category, b.category) && Objects.equals(a.images, b.images);
        }
    };
}
