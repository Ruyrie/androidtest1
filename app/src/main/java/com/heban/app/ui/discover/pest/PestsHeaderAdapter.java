package com.heban.app.ui.discover.pest;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;
import com.heban.app.databinding.PestsHeaderBinding;

import java.util.function.Consumer;

/** D04 列表头（只有一项，跨满两列）：内容通过 DataBinding 直接绑定 PestsViewModel。 */
public class PestsHeaderAdapter extends RecyclerView.Adapter<PestsHeaderAdapter.Holder> {
    static final int VIEW_TYPE = R.layout.pests_header;

    private final PestsViewModel vm;
    private final LifecycleOwner owner;
    private final Consumer<PestsHeaderBinding> onCreated;

    public PestsHeaderAdapter(@NonNull PestsViewModel vm, @NonNull LifecycleOwner owner,
                              @NonNull Consumer<PestsHeaderBinding> onCreated) {
        this.vm = vm;
        this.owner = owner;
        this.onCreated = onCreated;
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PestsHeaderBinding b = PestsHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        b.setVm(vm);
        b.setLifecycleOwner(owner);
        onCreated.accept(b);
        return new Holder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final PestsHeaderBinding binding;

        Holder(PestsHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
