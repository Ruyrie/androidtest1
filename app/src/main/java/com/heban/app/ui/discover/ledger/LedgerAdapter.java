package com.heban.app.ui.discover.ledger;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.databinding.LedgerItemDayBinding;
import com.heban.app.databinding.LedgerItemEntryBinding;

/** D10 明细列表：按天分组的标题行 + 流水（同一天的流水拼成一张圆角卡片）。 */
public class LedgerAdapter extends ListAdapter<LedgerRow, RecyclerView.ViewHolder> {

    /** 点击 / 长按一条流水 */
    public interface Listener {
        void onClick(@NonNull LedgerRow row);

        void onLongClick(@NonNull LedgerRow row);
    }

    private static final DiffUtil.ItemCallback<LedgerRow> DIFF = new DiffUtil.ItemCallback<LedgerRow>() {
        @Override
        public boolean areItemsTheSame(@NonNull LedgerRow a, @NonNull LedgerRow b) {
            return a.type == b.type && a.key == b.key;
        }

        @Override
        public boolean areContentsTheSame(@NonNull LedgerRow a, @NonNull LedgerRow b) {
            return a.equals(b);
        }
    };

    private final Listener listener;

    public LedgerAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        LedgerRow r = getItem(position);
        return r.type == LedgerRow.TYPE_DAY ? r.key : r.key + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == LedgerRow.TYPE_DAY) {
            return new DayHolder(LedgerItemDayBinding.inflate(inflater, parent, false));
        }
        return new EntryHolder(LedgerItemEntryBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LedgerRow row = getItem(position);
        if (holder instanceof DayHolder) {
            ((DayHolder) holder).bind(row);
        } else if (holder instanceof EntryHolder) {
            ((EntryHolder) holder).bind(row);
        }
    }

    static final class DayHolder extends RecyclerView.ViewHolder {
        private final LedgerItemDayBinding b;

        DayHolder(LedgerItemDayBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(LedgerRow row) {
            b.setRow(row);
            b.executePendingBindings();
        }
    }

    final class EntryHolder extends RecyclerView.ViewHolder {
        private final LedgerItemEntryBinding b;

        EntryHolder(LedgerItemEntryBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(LedgerRow row) {
            b.setRow(row);
            View item = b.item;
            item.setBackgroundResource(background(row));
            b.divider.setVisibility(row.first ? View.GONE : View.VISIBLE);

            // 收入：Secondary Container 圆底；支出：Surface Container Highest 圆底（原型 Ledger）
            int bg = MaterialColors.getColor(item, row.income
                    ? com.google.android.material.R.attr.colorSecondaryContainer
                    : com.google.android.material.R.attr.colorSurfaceContainerHighest);
            int fg = MaterialColors.getColor(item, row.income
                    ? com.google.android.material.R.attr.colorOnSecondaryContainer
                    : com.google.android.material.R.attr.colorOnSurfaceVariant);
            b.iconBg.setBackgroundTintList(ColorStateList.valueOf(bg));
            b.icon.setImageResource(row.icon);
            b.icon.setImageTintList(ColorStateList.valueOf(fg));
            b.tvAmount.setTextColor(MaterialColors.getColor(item, row.income
                    ? androidx.appcompat.R.attr.colorPrimary
                    : com.google.android.material.R.attr.colorOnSurface));

            item.setOnClickListener(v -> listener.onClick(row));
            item.setOnLongClickListener(v -> {
                listener.onLongClick(row);
                return true;
            });
            b.executePendingBindings();
        }

        private int background(LedgerRow row) {
            if (row.first && row.last) return R.drawable.ledger_bg_item_single;
            if (row.first) return R.drawable.ledger_bg_item_top;
            if (row.last) return R.drawable.ledger_bg_item_bottom;
            return R.drawable.ledger_bg_item_middle;
        }
    }
}
