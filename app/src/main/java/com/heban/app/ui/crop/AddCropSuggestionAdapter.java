package com.heban.app.ui.crop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.heban.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 输入框下拉建议：按「包含」匹配（输入「番」也能匹配「樱桃番茄」），每项可带图标。
 * C03 作物名称（生长期表，带作物图标）与 C04 物料常用名称共用。
 */
public final class AddCropSuggestionAdapter extends BaseAdapter implements Filterable {
    public static final class Item {
        @NonNull public final String label;
        @DrawableRes public final int icon;

        public Item(@NonNull String label, @DrawableRes int icon) {
            this.label = label;
            this.icon = icon;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }

    private final LayoutInflater inflater;
    private List<Item> all = Collections.emptyList();
    private List<Item> shown = Collections.emptyList();
    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String q = constraint == null ? "" : constraint.toString().trim();
            List<Item> out = new ArrayList<>();
            for (Item item : all) {
                if (q.isEmpty() || item.label.contains(q)) out.add(item);
            }
            FilterResults r = new FilterResults();
            r.values = out;
            r.count = out.size();
            return r;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            shown = results.values == null ? Collections.emptyList() : (List<Item>) results.values;
            if (results.count > 0) notifyDataSetChanged();
            else notifyDataSetInvalidated();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return resultValue instanceof Item ? ((Item) resultValue).label : super.convertResultToString(resultValue);
        }
    };

    public AddCropSuggestionAdapter(@NonNull Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setItems(@NonNull List<Item> items) {
        all = new ArrayList<>(items);
        shown = all;
        notifyDataSetChanged();
    }

    /** 搜索图标点击时显示全部候选 */
    public void showAll() {
        shown = all;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return shown.size();
    }

    @Override
    public Item getItem(int position) {
        return shown.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView != null ? convertView
                : inflater.inflate(R.layout.add_crop_item_suggestion, parent, false);
        Item item = getItem(position);
        ImageView icon = v.findViewById(R.id.icon);
        if (item.icon != 0) {
            icon.setImageResource(item.icon);
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
        }
        ((TextView) v.findViewById(R.id.text)).setText(item.label);
        return v;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }
}
