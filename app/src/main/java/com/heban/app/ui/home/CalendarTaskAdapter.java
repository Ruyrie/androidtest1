package com.heban.app.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.model.TaskItem;
import com.heban.app.databinding.CalendarItemTaskBinding;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** H03 当日农事列表：点击整行切换完成，长按删除。 */
public final class CalendarTaskAdapter extends ListAdapter<TaskItem, CalendarTaskAdapter.Holder> {

    public interface Listener {
        void onToggle(@NonNull TaskItem task);

        void onLongPress(@NonNull TaskItem task);
    }

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

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

    private final Listener listener;

    public CalendarTaskAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(CalendarItemTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        TaskItem task = getItem(position);
        CalendarItemTaskBinding b = h.binding;
        Context ctx = b.getRoot().getContext();
        b.setTask(task);
        b.executePendingBindings();

        CalendarTypeStyle style = CalendarTypeStyle.of(task.type);
        b.ivType.setImageResource(CalendarTypeStyle.listIcon(task.type));
        b.ivType.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(b.ivType, style.container)));
        b.ivType.setImageTintList(ColorStateList.valueOf(MaterialColors.getColor(b.ivType, style.onContainer)));

        b.tvMeta.setText(meta(ctx, task));

        b.ivDone.setImageResource(task.done ? R.drawable.ic_check_circle_fill : R.drawable.ic_radio_button_unchecked);
        b.ivDone.setImageTintList(ColorStateList.valueOf(MaterialColors.getColor(b.ivDone, task.done
                ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOutline)));

        b.row.setStateDescription(ctx.getString(task.done ? R.string.calendar_task_done : R.string.calendar_task_undone));
        b.row.setOnClickListener(v -> listener.onToggle(task));
        b.row.setOnLongClickListener(v -> {
            listener.onLongPress(task);
            return true;
        });
    }

    /** 「1号大棚 · 14:00」：没有地块时显示作物名，都没有时只显示时间 */
    static String meta(Context ctx, TaskItem task) {
        String time = CalendarDates.timeOf(task.dueAt, ZoneId.systemDefault()).format(HM);
        String place = task.plotName != null && !task.plotName.isEmpty() ? task.plotName : task.cropName;
        if (place == null || place.isEmpty()) return time;
        return ctx.getString(R.string.calendar_task_meta, place, time);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final CalendarItemTaskBinding binding;

        Holder(CalendarItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
