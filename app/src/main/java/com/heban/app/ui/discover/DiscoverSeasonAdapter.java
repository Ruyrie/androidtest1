package com.heban.app.ui.discover;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.ui.discover.pest.PestsFormat;
import com.heban.app.ui.discover.pest.PestsIcons;

import java.util.Objects;

/**
 * D01「本季高发」横向卡片：高发用 Error 配色（图标 Error，标签 Error Container），
 * 较高用 Warning 配色（图标 Warning，标签 Warning Container）。
 */
public class DiscoverSeasonAdapter extends ListAdapter<PestEntity, DiscoverSeasonAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull PestEntity pest);
    }

    private final Listener listener;

    public DiscoverSeasonAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.discover_item_season, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        PestEntity p = getItem(position);
        View v = h.itemView;
        boolean high = PestsFormat.isHigh(p.seasonLevel);
        int iconColor = MaterialColors.getColor(v, high ? androidx.appcompat.R.attr.colorError : R.attr.colorWarning);
        int levelBg = MaterialColors.getColor(v, high
                ? com.google.android.material.R.attr.colorErrorContainer : R.attr.colorWarningContainer);
        int levelFg = MaterialColors.getColor(v, high
                ? com.google.android.material.R.attr.colorOnErrorContainer : R.attr.colorOnWarningContainer);

        h.icon.setImageResource(PestsIcons.forPest(p));
        h.icon.setImageTintList(ColorStateList.valueOf(iconColor));
        String level = p.seasonLevel == null ? "" : p.seasonLevel.trim();
        h.level.setVisibility(level.isEmpty() ? View.GONE : View.VISIBLE);
        h.level.setText(level);
        h.level.setBackgroundTintList(ColorStateList.valueOf(levelBg));
        h.level.setTextColor(levelFg);
        h.name.setText(p.name);
        String crops = DiscoverFormat.cropsDot(p.cropTags);
        h.crops.setText(crops);
        h.crops.setVisibility(crops.isEmpty() ? View.GONE : View.VISIBLE);
        v.setContentDescription(PestsFormat.join("，", p.name, level, crops));
        v.setOnClickListener(x -> listener.onOpen(p));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView level;
        final TextView name;
        final TextView crops;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            level = v.findViewById(R.id.level);
            name = v.findViewById(R.id.name);
            crops = v.findViewById(R.id.crops);
        }
    }

    private static final DiffUtil.ItemCallback<PestEntity> DIFF = new DiffUtil.ItemCallback<PestEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull PestEntity a, @NonNull PestEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PestEntity a, @NonNull PestEntity b) {
            return Objects.equals(a.name, b.name) && Objects.equals(a.seasonLevel, b.seasonLevel)
                    && Objects.equals(a.cropTags, b.cropTags) && Objects.equals(a.icon, b.icon)
                    && Objects.equals(a.kind, b.kind);
        }
    };
}
