package com.heban.app.ui.crop;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.PlotCard;
import com.heban.app.databinding.PlotsItemCardBinding;

import java.time.ZoneId;

/** C05 地块卡片列表。 */
public class PlotsAdapter extends ListAdapter<PlotCard, PlotsAdapter.Holder> {

    public interface Listener {
        void onPlotClick(PlotCard card);

        void onMoreClick(View anchor, PlotCard card);

        void onCropClick(CropCard crop);

        void onReadingClick(PlotCard card);
    }

    private final Listener listener;
    private final ZoneId zone = ZoneId.systemDefault();

    public PlotsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PlotsItemCardBinding b = PlotsItemCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        Holder h = new Holder(b);
        b.getRoot().setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onPlotClick(getItem(pos));
        });
        b.btnMore.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onMoreClick(v, getItem(pos));
        });
        b.tvFooter.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            PlotCard card = getItem(pos);
            if (isGreenhouse(card.plot)) listener.onReadingClick(card);
        });
        return h;
    }

    static boolean isGreenhouse(PlotEntity p) {
        return p != null && !PlotEntity.TYPE_OPEN.equals(p.type);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PlotCard card = getItem(position);
        PlotsItemCardBinding b = holder.binding;
        Context ctx = b.getRoot().getContext();
        PlotEntity p = card.plot;
        boolean greenhouse = isGreenhouse(p);

        b.setName(p.name);
        String structure = p.structure != null && !p.structure.trim().isEmpty() ? p.structure : p.soil;
        b.setDesc(CropsFormat.join(CropsFormat.areaMu(p.areaMu), structure, p.irrigation));
        b.btnMore.setContentDescription(ctx.getString(R.string.plots_more, p.name));

        // 类型图标：大棚 warehouse（Secondary Container），露天 landscape（Warning Container）
        b.typeIcon.setImageResource(greenhouse ? R.drawable.ic_warehouse : R.drawable.ic_landscape);
        tint(b.typeIconBg, greenhouse ? com.google.android.material.R.attr.colorSecondaryContainer
                : R.attr.colorWarningContainer);
        b.typeIcon.setImageTintList(ColorStateList.valueOf(color(b.typeIcon,
                greenhouse ? com.google.android.material.R.attr.colorOnSecondaryContainer
                        : R.attr.colorOnWarningContainer)));

        // 标签：湿度偏高优先，其次地块类型
        if (card.humidityHigh) {
            b.tag.setText(R.string.plots_humidity_high);
            tint(b.tag, com.google.android.material.R.attr.colorErrorContainer);
            b.tag.setTextColor(color(b.tag, com.google.android.material.R.attr.colorOnErrorContainer));
        } else if (greenhouse) {
            b.tag.setText(PlotEntity.TYPE_GREENHOUSE);
            tint(b.tag, com.google.android.material.R.attr.colorSecondaryContainer);
            b.tag.setTextColor(color(b.tag, com.google.android.material.R.attr.colorOnSecondaryContainer));
        } else {
            b.tag.setText(PlotEntity.TYPE_OPEN);
            tint(b.tag, R.attr.colorWarningContainer);
            b.tag.setTextColor(color(b.tag, R.attr.colorOnWarningContainer));
        }

        bindCrops(b, card);

        // 底部：大棚显示棚内温湿度（点击记录），露天显示上次灌溉
        String footer;
        int icon;
        if (greenhouse) {
            icon = R.drawable.plots_ic_thermostat_small;
            footer = p.indoorTemp != null && p.indoorHumidity != null
                    ? ctx.getString(R.string.plots_reading, CropsFormat.temperature(p.indoorTemp), p.indoorHumidity)
                    : ctx.getString(R.string.plots_reading_none);
        } else {
            icon = R.drawable.plots_ic_water_drop_small;
            footer = card.lastIrrigationAt != null
                    ? ctx.getString(R.string.plots_last_irrigation, CropsFormat.monthDayCn(card.lastIrrigationAt, zone))
                    : ctx.getString(R.string.plots_irrigation_none);
        }
        b.setFooter(footer);
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(b.tvFooter, icon, 0, 0, 0);
        b.tvFooter.setClickable(greenhouse);
        b.tvFooter.setFocusable(greenhouse);
        b.setCost(ctx.getString(R.string.plots_season_cost, CropsFormat.money(card.seasonCostCent)));
        b.executePendingBindings();
    }

    /** 在种作物 Chip：只有一种作物时显示阶段，多种作物时显示各自面积（原型） */
    private void bindCrops(PlotsItemCardBinding b, PlotCard card) {
        b.crops.removeAllViews();
        boolean empty = card.crops == null || card.crops.isEmpty();
        b.crops.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.tvNoCrops.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) return;
        Context ctx = b.crops.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        boolean single = card.crops.size() == 1;
        for (CropCard crop : card.crops) {
            Chip chip = (Chip) inflater.inflate(R.layout.plots_item_crop_chip, b.crops, false);
            chip.setChipIconResource(crop.icon != 0 ? crop.icon : R.drawable.ic_potted_plant);
            String second = single && crop.stage != null ? crop.stage : CropsFormat.areaMu(crop.areaMu);
            chip.setText(ctx.getString(R.string.plots_crop_stage, crop.name, second));
            chip.setOnClickListener(v -> listener.onCropClick(crop));
            b.crops.addView(chip);
        }
    }

    private static void tint(View v, @AttrRes int attr) {
        v.setBackgroundTintList(ColorStateList.valueOf(color(v, attr)));
    }

    private static int color(View v, @AttrRes int attr) {
        return MaterialColors.getColor(v, attr);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final PlotsItemCardBinding binding;

        Holder(PlotsItemCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final DiffUtil.ItemCallback<PlotCard> DIFF = new DiffUtil.ItemCallback<PlotCard>() {
        @Override
        public boolean areItemsTheSame(@NonNull PlotCard a, @NonNull PlotCard b) {
            return a.plot != null && b.plot != null && a.plot.id == b.plot.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PlotCard a, @NonNull PlotCard b) {
            return a.equals(b) && a.plot.type.equals(b.plot.type)
                    && java.util.Objects.equals(a.plot.soil, b.plot.soil);
        }
    };
}
