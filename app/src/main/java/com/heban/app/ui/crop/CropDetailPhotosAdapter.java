package com.heban.app.ui.crop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;
import com.heban.app.ui.common.BindingAdapters;

/** C02 照片 Tab：3 列方形缩略图（Glide centerCrop）。 */
public class CropDetailPhotosAdapter extends ListAdapter<String, CropDetailPhotosAdapter.Holder> {

    public CropDetailPhotosAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.crop_detail_item_photo, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BindingAdapters.loadImage(holder.photo, getItem(position), null);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView photo;

        Holder(View v) {
            super(v);
            photo = v.findViewById(R.id.photo);
        }
    }

    static final DiffUtil.ItemCallback<String> DIFF = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull String a, @NonNull String b) {
            return a.equals(b);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String a, @NonNull String b) {
            return a.equals(b);
        }
    };
}
