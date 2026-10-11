package com.heban.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;
import com.heban.app.data.model.CityOption;

import java.util.Objects;

/** 城市搜索结果列表。 */
public class CityPickerAdapter extends ListAdapter<CityOption, CityPickerAdapter.Holder> {

    public interface Listener {
        void onSelect(@NonNull CityOption city);
    }

    private final Listener listener;

    public CityPickerAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.weather_item_city, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CityOption c = getItem(position);
        h.name.setText(c.displayName());
        String region = region(c);
        h.region.setText(region);
        h.region.setVisibility(region.isEmpty() ? View.GONE : View.VISIBLE);
        h.itemView.setOnClickListener(v -> listener.onSelect(c));
    }

    /** 第二行：省份（与上级城市不同时加上城市），如「江苏 南京」 */
    static String region(CityOption c) {
        StringBuilder sb = new StringBuilder();
        if (c.adm1 != null && !c.adm1.isEmpty()) sb.append(c.adm1);
        if (c.adm2 != null && !c.adm2.isEmpty() && !c.adm2.equals(c.adm1)) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(c.adm2);
        }
        return sb.toString();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView region;

        Holder(View v) {
            super(v);
            name = v.findViewById(R.id.name);
            region = v.findViewById(R.id.region);
        }
    }

    private static final DiffUtil.ItemCallback<CityOption> DIFF = new DiffUtil.ItemCallback<CityOption>() {
        @Override
        public boolean areItemsTheSame(@NonNull CityOption a, @NonNull CityOption b) {
            return Objects.equals(a.id, b.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull CityOption a, @NonNull CityOption b) {
            return Objects.equals(a.id, b.id) && Objects.equals(a.name, b.name)
                    && Objects.equals(a.adm1, b.adm1) && Objects.equals(a.adm2, b.adm2);
        }
    };
}
