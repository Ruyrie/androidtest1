package com.heban.app.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;

import java.util.ArrayList;
import java.util.List;

/** H02 逐小时预报（横向）：一屏 6 格（与原型的 6 列一致），可左右滑动查看更多。 */
public class WeatherHourlyAdapter extends RecyclerView.Adapter<WeatherHourlyAdapter.Holder> {
    static final int VISIBLE_COLUMNS = 6;

    private final List<WeatherFormat.Hour> items = new ArrayList<>();

    @SuppressWarnings("NotifyDataSetChanged")
    void submit(@NonNull List<WeatherFormat.Hour> hours) {
        items.clear();
        items.addAll(hours);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.weather_item_hour, parent, false);
        int width = columnWidth((RecyclerView) parent);
        if (width > 0) v.getLayoutParams().width = width;
        return new Holder(v);
    }

    private static int columnWidth(RecyclerView rv) {
        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        int w = lm != null && lm.getWidth() > 0 ? lm.getWidth() : rv.getWidth();
        return (w - rv.getPaddingLeft() - rv.getPaddingRight()) / VISIBLE_COLUMNS;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        WeatherFormat.Hour hour = items.get(position);
        Context c = h.itemView.getContext();
        h.time.setText(hour.isNow() ? c.getString(R.string.weather_now) : WeatherFormat.hourLabel(c, hour.time));
        h.icon.setImageResource(hour.icon);
        h.temp.setText(WeatherFormat.temp(hour.temp));
        h.itemView.setContentDescription(h.time.getText() + " " + h.temp.getText());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView time;
        final ImageView icon;
        final TextView temp;

        Holder(View v) {
            super(v);
            time = v.findViewById(R.id.time);
            icon = v.findViewById(R.id.icon);
            temp = v.findViewById(R.id.temp);
        }
    }
}
