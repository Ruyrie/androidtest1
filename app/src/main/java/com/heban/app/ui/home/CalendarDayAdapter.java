package com.heban.app.ui.home;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;
import com.heban.app.databinding.CalendarItemDayBinding;

import java.util.Objects;

/** H03 月历格子（GridLayoutManager 7 列）。 */
public final class CalendarDayAdapter extends ListAdapter<CalendarDay, CalendarDayAdapter.Holder> {

    public interface OnDayClick {
        void onDayClick(@NonNull CalendarDay day);
    }

    private static final DiffUtil.ItemCallback<CalendarDay> DIFF = new DiffUtil.ItemCallback<CalendarDay>() {
        @Override
        public boolean areItemsTheSame(@NonNull CalendarDay a, @NonNull CalendarDay b) {
            return Objects.equals(a.date, b.date);
        }

        @Override
        public boolean areContentsTheSame(@NonNull CalendarDay a, @NonNull CalendarDay b) {
            return a.equals(b);
        }
    };

    private final OnDayClick onClick;

    public CalendarDayAdapter(@NonNull OnDayClick onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(CalendarItemDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CalendarDay day = getItem(position);
        h.binding.setDay(day);
        h.binding.executePendingBindings();
        h.binding.tvNumber.setTypeface(day.selected ? Typeface.create("sans-serif-medium", Typeface.NORMAL)
                : Typeface.DEFAULT);
        View root = h.binding.getRoot();
        if (day.isBlank()) {
            root.setOnClickListener(null);
            root.setClickable(false);
            root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            root.setContentDescription(null);
        } else {
            root.setOnClickListener(v -> onClick.onDayClick(day));
            root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            String desc = root.getContext().getString(R.string.calendar_day_desc,
                    day.date.getMonthValue(), day.date.getDayOfMonth());
            if (day.term != null) desc = desc + " " + day.term;
            root.setContentDescription(desc);
            root.setSelected(day.selected);
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final CalendarItemDayBinding binding;

        Holder(CalendarItemDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
