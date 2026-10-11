package com.heban.app.ui.home;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.model.FarmIndex;

import java.util.ArrayList;
import java.util.List;

/** H02「今日农事指数」3×2 宫格。 */
public class WeatherIndexAdapter extends RecyclerView.Adapter<WeatherIndexAdapter.Holder> {
    private final List<FarmIndex> items = new ArrayList<>();

    @SuppressWarnings("NotifyDataSetChanged")
    void submit(@NonNull List<FarmIndex> indices) {
        items.clear();
        items.addAll(indices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.weather_item_index, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        FarmIndex f = items.get(position);
        if (f.icon != 0) {
            h.icon.setImageResource(f.icon);
            h.icon.setVisibility(View.VISIBLE);
        } else {
            h.icon.setVisibility(View.GONE);
        }
        h.icon.setImageTintList(ColorStateList.valueOf(
                MaterialColors.getColor(h.icon, WeatherFormat.indexIconColor(f))));
        h.name.setText(f.name);
        h.level.setText(f.level);
        h.level.setTextColor(MaterialColors.getColor(h.level, WeatherFormat.indexLevelColor(f)));
        h.itemView.setContentDescription(f.name + " " + f.level
                + (f.detail == null || f.detail.isEmpty() ? "" : " " + f.detail));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView level;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            name = v.findViewById(R.id.name);
            level = v.findViewById(R.id.level);
        }
    }
}
