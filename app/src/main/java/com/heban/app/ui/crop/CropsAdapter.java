package com.heban.app.ui.crop;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.data.model.CropCard;
import com.heban.app.databinding.CropsItemCardBinding;

/** C01 作物卡片列表（ListAdapter + item DataBinding）。 */
public class CropsAdapter extends ListAdapter<CropCard, CropsAdapter.Holder> {

    public interface OnCropClick {
        void onClick(CropCard card);
    }

    private final OnCropClick onClick;

    public CropsAdapter(OnCropClick onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CropsItemCardBinding b = CropsItemCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        Holder h = new Holder(b);
        b.getRoot().setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) onClick.onClick(getItem(pos));
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.binding.setCard(getItem(position));
        holder.binding.executePendingBindings();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final CropsItemCardBinding binding;

        Holder(CropsItemCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final DiffUtil.ItemCallback<CropCard> DIFF = new DiffUtil.ItemCallback<CropCard>() {
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
