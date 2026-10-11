package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.CropCard;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

public class CropsFormatTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    public void areaKeepsOneDecimal() {
        assertEquals("3.0", CropsFormat.area(3));
        assertEquals("0.5", CropsFormat.area(0.5));
        assertEquals("0.25", CropsFormat.area(0.25));
        assertEquals("4.2", CropsFormat.area(4.2));
        assertEquals("0.5 亩", CropsFormat.areaMu(0.5));
    }

    @Test
    public void moneyFromCents() {
        assertEquals("¥1,240", CropsFormat.money(124000));
        assertEquals("¥960", CropsFormat.money(96000));
        assertEquals("¥12.5", CropsFormat.money(1250));
        assertEquals("¥0", CropsFormat.money(0));
    }

    @Test
    public void temperatureAndAmount() {
        assertEquals("22", CropsFormat.temperature(22.0));
        assertEquals("21.5", CropsFormat.temperature(21.5));
        assertEquals("5", CropsFormat.amount(5.0));
        assertEquals("2.5", CropsFormat.amount(2.5));
    }

    @Test
    public void dates() {
        long millis = LocalDate.of(2026, 10, 5).atStartOfDay(ZONE).toInstant().toEpochMilli();
        assertEquals("10/05", CropsFormat.monthDay(millis, ZONE));
        assertEquals("10月05日", CropsFormat.monthDayCn(millis, ZONE));
    }

    @Test
    public void cardSubtitleFollowsPrototype() {
        CropCard tomato = CropsFakeRepository.card(1, "番茄", "樱桃番茄", "1号大棚", 0.5, "大棚", "结果期", 68, 110, null);
        assertEquals("番茄 · 樱桃番茄", CropsFormat.cropTitle(tomato.name, tomato.variety));
        assertEquals("1号大棚 · 0.5 亩", CropsFormat.cropSubtitle(tomato));

        CropCard cucumber = CropsFakeRepository.card(2, "黄瓜", null, "2号大棚", 0.4, "大棚", "开花期", 35, 78, "霜霉病风险");
        assertEquals("黄瓜", CropsFormat.cropTitle(cucumber.name, cucumber.variety));
        assertEquals("2号大棚 · 0.4 亩 · 开花期", CropsFormat.cropSubtitle(cucumber));

        CropCard wheat = CropsFakeRepository.card(3, "冬小麦", null, "东地块", 3.0, "露天", "分蘖期", 21, 240, null);
        assertEquals("东地块 · 3.0 亩 · 露天", CropsFormat.cropSubtitle(wheat));

        CropCard pot = CropsFakeRepository.card(5, "辣椒", null, null, 0.1, "盆栽", "苗期", 3, 100, null);
        assertEquals("0.1 亩 · 盆栽", CropsFormat.cropSubtitle(pot));
    }

    @Test
    public void stepLabelDropsTrailingPeriodChar() {
        assertEquals("结果", CropsFormat.stepLabel("结果期"));
        assertEquals("育苗", CropsFormat.stepLabel("育苗"));
        assertEquals("苗期", CropsFormat.stepLabel("苗期"));
    }

    @Test
    public void recordSubtitle() {
        FarmRecordEntity r = new FarmRecordEntity();
        r.material = "硫酸钾";
        r.amount = 5;
        r.unit = "kg";
        assertEquals("硫酸钾 5 kg", CropsFormat.recordSubtitle(r));
        r.costCent = 12000;
        assertEquals("硫酸钾 5 kg · ¥120", CropsFormat.recordSubtitle(r));
        FarmRecordEntity n = new FarmRecordEntity();
        n.note = "整枝打杈，摘除老叶";
        assertEquals("整枝打杈，摘除老叶", CropsFormat.recordSubtitle(n));
    }

    @Test
    public void parsePhotosToleratesBadJson() {
        Gson gson = new Gson();
        assertEquals(Arrays.asList("a.jpg", "b.jpg"), CropsFormat.parsePhotos(gson, "[\"a.jpg\",\"\",\"b.jpg\"]"));
        assertTrue(CropsFormat.parsePhotos(gson, null).isEmpty());
        assertTrue(CropsFormat.parsePhotos(gson, "not json").isEmpty());
        assertTrue(CropsFormat.parsePhotos(gson, "{\"a\":1}").isEmpty());
    }
}
