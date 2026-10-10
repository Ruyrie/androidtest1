package com.heban.app.data.repository;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.CropStats;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** 由作物、地块和生长期表生成 C01 / C02 / H01 展示数据（纯 Java）。 */
public final class CropCards {
    private CropCards() {
    }

    /** 作物的预计天数：未填写时取生长期表 */
    public static int expectedDays(CropEntity crop, CropProfile profile) {
        return crop.expectedDays > 0 ? crop.expectedDays : profile.expectedDays;
    }

    public static CropCard card(CropEntity crop, @Nullable PlotEntity plot, CropProfile profile,
                                @DrawableRes int icon, LocalDate today, ZoneId zone) {
        CropCard c = new CropCard();
        c.id = crop.id;
        c.name = crop.name;
        c.variety = crop.variety;
        c.icon = icon;
        c.coverPath = crop.coverPath != null && !crop.coverPath.isEmpty() ? crop.coverPath : profile.image;
        c.expectedDays = expectedDays(crop, profile);
        c.daysPlanted = GrowthCalculator.daysPlanted(crop.plantedAt, today, zone);
        c.progress = GrowthCalculator.progress(c.daysPlanted, c.expectedDays);
        c.stage = GrowthCalculator.stage(profile, c.daysPlanted, c.expectedDays);
        c.plotName = plot == null ? null : plot.name;
        c.areaMu = crop.areaMu;
        c.plantMode = crop.plantMode;
        c.attentionNote = crop.attentionNote != null && !crop.attentionNote.isEmpty() ? crop.attentionNote : null;
        return c;
    }

    /** C01 筛选：全部 / 大棚 / 露天（按种植方式）/ 需关注 */
    public static boolean matches(CropCard card, @Nullable String filter) {
        if (filter == null) return true;
        switch (filter) {
            case CropRepository.FILTER_GREENHOUSE:
                return CropEntity.MODE_GREENHOUSE.equals(card.plantMode);
            case CropRepository.FILTER_OPEN:
                return CropEntity.MODE_OPEN.equals(card.plantMode);
            case CropRepository.FILTER_ATTENTION:
                return card.attentionNote != null;
            default:
                return true;
        }
    }

    public static List<CropCard> filter(List<CropCard> cards, @Nullable String filter) {
        List<CropCard> out = new ArrayList<>(cards.size());
        for (CropCard c : cards) {
            if (matches(c, filter)) out.add(c);
        }
        return out;
    }

    /** C01 统计块：在种作物 / 本周农事 / 需关注 */
    public static CropStats stats(List<CropEntity> crops, int weekTasks) {
        CropStats s = new CropStats();
        s.growing = crops.size();
        s.weekTasks = weekTasks;
        for (CropEntity c : crops) {
            if (c.attentionNote != null && !c.attentionNote.isEmpty()) s.attention++;
        }
        return s;
    }

    public static boolean sameStats(CropStats a, CropStats b) {
        return a.growing == b.growing && a.weekTasks == b.weekTasks && a.attention == b.attention;
    }

    /** C02：阶段步进条、预计还需天数、预计采收日期和农事记录（记录按日期倒序传入） */
    public static CropDetail detail(CropEntity crop, @Nullable PlotEntity plot, CropCard card, CropProfile profile,
                                    List<FarmRecordEntity> records, ZoneId zone) {
        CropDetail d = new CropDetail();
        d.crop = crop;
        d.plotName = card.plotName;
        d.card = card;
        d.stages = GrowthCalculator.stageInfos(profile, card.daysPlanted, card.expectedDays);
        d.daysLeft = GrowthCalculator.daysLeft(card.daysPlanted, card.expectedDays);
        d.expectedHarvestAt = GrowthCalculator.expectedHarvestAt(crop.plantedAt, card.expectedDays, zone);
        d.records = new ArrayList<>(records);
        return d;
    }
}
