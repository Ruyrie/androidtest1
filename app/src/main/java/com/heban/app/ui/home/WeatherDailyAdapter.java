package com.heban.app.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.util.WeatherIcons;

import java.util.ArrayList;
import java.util.List;

/** H02「未来 7 天」：日期、图标、最低温、温度条（以一周最低 / 最高温为两端）、最高温。 */
public class WeatherDailyAdapter extends RecyclerView.Adapter<WeatherDailyAdapter.Holder> {
    private final List<DailyWeather> items = new ArrayList<>();
    private int weekMin;
    private int weekMax;
    private long now;

    @SuppressWarnings("NotifyDataSetChanged")
    void submit(@NonNull List<DailyWeather> days, long now) {
        items.clear();
        items.addAll(days);
        this.now = now;
        weekMin = Integer.MAX_VALUE;
        weekMax = Integer.MIN_VALUE;
        for (DailyWeather d : days) {
            weekMin = Math.min(weekMin, d.tempMin);
            weekMax = Math.max(weekMax, d.tempMax);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.weather_item_day, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        DailyWeather d = items.get(position);
        Context c = h.itemView.getContext();
        h.day.setText(WeatherFormat.dayLabel(c, d.date, now));
        h.day.setTypeface(WeatherFormat.isToday(d.date, now)
                ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.DEFAULT);
        int icon = WeatherIcons.forCode(d.iconDay);
        h.icon.setImageResource(icon);
        h.icon.setImageTintList(ColorStateList.valueOf(
                MaterialColors.getColor(h.icon, WeatherFormat.dayIconColor(icon))));
        h.min.setText(WeatherFormat.temp(d.tempMin));
        h.max.setText(WeatherFormat.temp(d.tempMax));
        float[] r = WeatherFormat.range(d.tempMin, d.tempMax, weekMin, weekMax);
        h.bar.setRange(r[0], r[1], MaterialColors.getColor(h.bar, WeatherFormat.isCold(d)
                ? com.google.android.material.R.attr.colorTertiary
                : androidx.appcompat.R.attr.colorPrimary));
        h.itemView.setContentDescription(h.day.getText() + " " + (d.textDay == null ? "" : d.textDay)
                + " " + h.min.getText() + " " + h.max.getText());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView day;
        final ImageView icon;
        final TextView min;
        final WeatherRangeBar bar;
        final TextView max;

        Holder(View v) {
            super(v);
            day = v.findViewById(R.id.day);
            icon = v.findViewById(R.id.icon);
            min = v.findViewById(R.id.min);
            bar = v.findViewById(R.id.bar);
            max = v.findViewById(R.id.max);
        }
    }
}
