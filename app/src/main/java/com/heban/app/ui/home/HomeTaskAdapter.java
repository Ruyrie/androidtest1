package com.heban.app.ui.home;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.model.TaskItem;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** H01「今日农事」列表：复选框（整行点击切换）、删除线、「1号大棚 · 已完成 / 14:00」、需关注标签。 */
public class HomeTaskAdapter extends ListAdapter<TaskItem, HomeTaskAdapter.Holder> {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public interface Listener {
        void onToggle(@NonNull TaskItem task);
    }

    private final Listener listener;

    public HomeTaskAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.home_item_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        TaskItem t = getItem(position);
        Context c = h.itemView.getContext();
        h.divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        h.check.setChecked(t.done);
        h.title.setText(t.title);
        h.title.setTextColor(MaterialColors.getColor(h.title, t.done
                ? com.google.android.material.R.attr.colorOnSurfaceVariant
                : com.google.android.material.R.attr.colorOnSurface));
        int flags = h.title.getPaintFlags();
        h.title.setPaintFlags(t.done ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        h.subtitle.setText(subtitle(c, t));
        h.attention.setVisibility(t.attention && !t.done ? View.VISIBLE : View.GONE);
        h.row.setOnClickListener(v -> listener.onToggle(t));
        h.row.setContentDescription(c.getString(R.string.home_cd_item, t.title, subtitle(c, t)));
        h.row.setStateDescription(t.done ? c.getString(R.string.home_task_done) : null);
    }

    /** 「1号大棚 · 已完成」「2号大棚 · 14:00」 */
    static String subtitle(Context c, TaskItem t) {
        String where = t.plotName != null && !t.plotName.isEmpty() ? t.plotName : t.cropName;
        String when = t.done ? c.getString(R.string.home_task_done) : time(t.dueAt);
        if (where == null || where.isEmpty()) return when;
        return c.getString(R.string.home_date_location, where, when);
    }

    private static String time(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(HH_MM);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View divider;
        final View row;
        final CheckBox check;
        final TextView title;
        final TextView subtitle;
        final TextView attention;

        Holder(View v) {
            super(v);
            divider = v.findViewById(R.id.divider);
            row = v.findViewById(R.id.row);
            check = v.findViewById(R.id.check);
            title = v.findViewById(R.id.title);
            subtitle = v.findViewById(R.id.subtitle);
            attention = v.findViewById(R.id.attention);
        }
    }

    private static final DiffUtil.ItemCallback<TaskItem> DIFF = new DiffUtil.ItemCallback<TaskItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskItem a, @NonNull TaskItem b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskItem a, @NonNull TaskItem b) {
            return a.equals(b);
        }
    };
}
