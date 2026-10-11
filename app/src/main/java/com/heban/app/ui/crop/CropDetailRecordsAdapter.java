package com.heban.app.ui.crop;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.FarmType;
import com.heban.app.databinding.CropDetailItemRecordBinding;

import java.time.ZoneId;
import java.util.Objects;

/** C02 农事记录列表：类型图标 / 名称、物料用量与金额、日期（MM/dd）；长按删除。 */
public class CropDetailRecordsAdapter extends ListAdapter<FarmRecordEntity, CropDetailRecordsAdapter.Holder> {

    public interface OnRecordLongClick {
        void onLongClick(FarmRecordEntity record);
    }

    private final OnRecordLongClick onLongClick;
    private final ZoneId zone = ZoneId.systemDefault();

    public CropDetailRecordsAdapter(OnRecordLongClick onLongClick) {
        super(DIFF);
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CropDetailItemRecordBinding b = CropDetailItemRecordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        Holder h = new Holder(b);
        b.getRoot().setOnLongClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return false;
            onLongClick.onLongClick(getItem(pos));
            return true;
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        FarmRecordEntity r = getItem(position);
        holder.binding.setType(FarmType.of(r.type));
        holder.binding.setSubtitle(CropsFormat.recordSubtitle(r));
        holder.binding.setDate(CropsFormat.monthDay(r.date, zone));
        holder.binding.executePendingBindings();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final CropDetailItemRecordBinding binding;

        Holder(CropDetailItemRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final DiffUtil.ItemCallback<FarmRecordEntity> DIFF = new DiffUtil.ItemCallback<FarmRecordEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull FarmRecordEntity a, @NonNull FarmRecordEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull FarmRecordEntity a, @NonNull FarmRecordEntity b) {
            return a.date == b.date && a.costCent == b.costCent && Double.compare(a.amount, b.amount) == 0
                    && Objects.equals(a.type, b.type) && Objects.equals(a.material, b.material)
                    && Objects.equals(a.unit, b.unit) && Objects.equals(a.note, b.note)
                    && Objects.equals(a.photos, b.photos);
        }
    };
}
