package com.heban.app.ui.home;

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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.heban.app.R;
import com.heban.app.data.model.CropCard;
import com.heban.app.util.ImagePaths;

/** H01「我的作物」横向卡片：有封面图时用 Glide 加载，否则显示作物图标占位（同原型）。 */
public class HomeCropAdapter extends ListAdapter<CropCard, HomeCropAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull CropCard crop);
    }

    private final Listener listener;

    public HomeCropAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.home_item_crop, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CropCard c = getItem(position);
        h.icon.setImageResource(c.icon != 0 ? c.icon : R.drawable.ic_potted_plant);
        Uri cover = ImagePaths.toUri(c.coverPath);
        if (cover == null) {
            Glide.with(h.cover).clear(h.cover);
            h.cover.setImageDrawable(null);
            h.cover.setVisibility(View.GONE);
        } else {
            h.cover.setVisibility(View.VISIBLE);
            Glide.with(h.cover).load(cover).centerCrop().into(h.cover);
        }
        h.name.setText(c.name);
        String stage = c.stage == null || c.stage.isEmpty()
                ? h.itemView.getContext().getString(R.string.home_crop_day, c.daysPlanted)
                : h.itemView.getContext().getString(R.string.home_crop_stage_day, c.stage, c.daysPlanted);
        h.stage.setText(stage);
        h.progress.setProgress(Math.max(0, Math.min(100, c.progress)));
        h.itemView.setOnClickListener(v -> listener.onOpen(c));
        h.itemView.setContentDescription(h.itemView.getContext().getString(R.string.home_cd_item, c.name, stage));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final ImageView cover;
        final TextView name;
        final TextView stage;
        final LinearProgressIndicator progress;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            cover = v.findViewById(R.id.cover);
            name = v.findViewById(R.id.name);
            stage = v.findViewById(R.id.stage);
            progress = v.findViewById(R.id.progress);
        }
    }

    private static final DiffUtil.ItemCallback<CropCard> DIFF = new DiffUtil.ItemCallback<CropCard>() {
        @Override
        public boolean areItemsTheSame(@NonNull CropCard a, @NonNull CropCard b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull CropCard a, @NonNull CropCard b) {
            return a.equals(b);
        }
    };
}
