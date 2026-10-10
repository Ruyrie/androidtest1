package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.local.dao.PlotValue;
import com.heban.app.data.local.dao.TaskDayType;
import com.heban.app.data.local.dao.TaskRow;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;
import com.heban.app.data.model.TaskItem;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** CropCards / PlotCards / TaskItems 组装逻辑，数据取自原型 C01 / C02 / C05 / H03。 */
public class CardsTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);

    private static PlotEntity plot(long id, String name, double area, Integer humidity) {
        PlotEntity p = new PlotEntity();
        p.id = id;
        p.name = name;
        p.areaMu = area;
        p.indoorHumidity = humidity;
        return p;
    }

    private static CropEntity crop(long id, String name, String variety, Long plotId, double area,
                                   String mode, int daysPlanted, String attention) {
        CropEntity c = new CropEntity();
        c.id = id;
        c.name = name;
        c.variety = variety;
        c.plotId = plotId;
        c.areaMu = area;
        c.plantMode = mode;
        c.plantedAt = DateRanges.startOfDay(TODAY.minusDays(daysPlanted - 1), ZONE);
        c.attentionNote = attention;
        return c;
    }

    private static PlotValue value(long plotId, long v) {
        PlotValue p = new PlotValue();
        p.plotId = plotId;
        p.value = v;
        return p;
    }

    private List<PlotEntity> plots() {
        return Arrays.asList(plot(1, "1号大棚", 0.5, 76), plot(2, "2号大棚", 0.7, 89), plot(3, "东地块", 3.0, null));
    }

    private List<CropEntity> crops() {
        return Arrays.asList(
                crop(1, "番茄", "樱桃番茄", 1L, 0.5, CropEntity.MODE_GREENHOUSE, 68, null),
                crop(2, "黄瓜", null, 2L, 0.4, CropEntity.MODE_GREENHOUSE, 35, "霜霉病风险"),
                crop(3, "冬小麦", null, 3L, 3.0, CropEntity.MODE_OPEN, 21, null),
                crop(4, "草莓", "红颜", 2L, 0.3, CropEntity.MODE_GREENHOUSE, 3, null));
    }

    private List<CropCard> cards() throws Exception {
        Map<Long, PlotEntity> byId = new HashMap<>();
        for (PlotEntity p : plots()) byId.put(p.id, p);
        List<CropCard> out = new ArrayList<>();
        for (CropEntity c : crops()) {
            out.add(CropCards.card(c, byId.get(c.plotId),
                    CropProfiles.match(ProfileFixture.profiles(), c.name, c.variety), 0, TODAY, ZONE));
        }
        return out;
    }

    @Test
    public void cropCardsMatchPrototype() throws Exception {
        List<CropCard> cards = cards();
        CropCard tomato = cards.get(0);
        assertEquals("结果期", tomato.stage);
        assertEquals(68, tomato.daysPlanted);
        assertEquals(110, tomato.expectedDays);
        assertEquals(61, tomato.progress);
        assertEquals("1号大棚", tomato.plotName);
        assertEquals("images/crops/cherry_tomato.jpg", tomato.coverPath);
        CropCard cucumber = cards.get(1);
        assertEquals("开花期", cucumber.stage);
        assertEquals("35/78", cucumber.daysPlanted + "/" + cucumber.expectedDays);
        assertEquals("霜霉病风险", cucumber.attentionNote);
        assertEquals("分蘖期", cards.get(2).stage);
        assertEquals("21/240", cards.get(2).daysPlanted + "/" + cards.get(2).expectedDays);
        assertEquals("定植期", cards.get(3).stage);
        assertEquals("3/150", cards.get(3).daysPlanted + "/" + cards.get(3).expectedDays);
    }

    @Test
    public void ownCoverWins() throws Exception {
        CropEntity c = crops().get(0);
        c.coverPath = "file:///data/cover.jpg";
        CropCard card = CropCards.card(c, null, ProfileFixture.profile("番茄"), 0, TODAY, ZONE);
        assertEquals("file:///data/cover.jpg", card.coverPath);
        assertNull(card.plotName);
    }

    @Test
    public void filters() throws Exception {
        List<CropCard> cards = cards();
        assertEquals(4, CropCards.filter(cards, null).size());
        assertEquals(4, CropCards.filter(cards, CropRepository.FILTER_ALL).size());
        assertEquals(3, CropCards.filter(cards, CropRepository.FILTER_GREENHOUSE).size());
        assertEquals(1, CropCards.filter(cards, CropRepository.FILTER_OPEN).size());
        assertEquals("冬小麦", CropCards.filter(cards, CropRepository.FILTER_OPEN).get(0).name);
        assertEquals(1, CropCards.filter(cards, CropRepository.FILTER_ATTENTION).size());
        assertEquals("黄瓜", CropCards.filter(cards, CropRepository.FILTER_ATTENTION).get(0).name);
    }

    @Test
    public void cropStats() {
        CropStats s = CropCards.stats(crops(), 12);
        assertEquals(4, s.growing);
        assertEquals(12, s.weekTasks);
        assertEquals(1, s.attention);
    }

    @Test
    public void cropDetail() throws Exception {
        CropEntity c = crops().get(0);
        CropCard card = cards().get(0);
        FarmRecordEntity r = new FarmRecordEntity();
        r.type = "FERTILIZE";
        CropDetail d = CropCards.detail(c, plots().get(0), card, ProfileFixture.profile("樱桃番茄"),
                Collections.singletonList(r), ZONE);
        assertEquals(42, d.daysLeft);
        assertEquals(DateRanges.startOfDay(LocalDate.of(2026, 11, 21), ZONE), d.expectedHarvestAt);
        assertEquals(5, d.stages.size());
        assertTrue(d.stages.get(3).current);
        assertEquals("1号大棚", d.plotName);
        assertEquals(1, d.records.size());
    }

    @Test
    public void plotCardsAndStats() throws Exception {
        List<CropCard> cards = cards();
        Map<Long, CropCard> byCrop = new HashMap<>();
        for (CropCard c : cards) byCrop.put(c.id, c);
        long irrigated = LocalDateTime.of(2026, 10, 5, 9, 0).atZone(ZONE).toInstant().toEpochMilli();
        List<PlotCard> pc = PlotCards.cards(plots(), crops(), byCrop,
                Arrays.asList(value(1, 124000), value(2, 96000), value(3, 218000)),
                Collections.singletonList(value(3, irrigated)));
        assertEquals(3, pc.size());
        assertEquals(1, pc.get(0).crops.size());
        assertEquals(124000, pc.get(0).seasonCostCent);
        assertFalse(pc.get(0).humidityHigh);
        assertNull(pc.get(0).lastIrrigationAt);
        assertEquals(2, pc.get(1).crops.size());
        assertTrue(pc.get(1).humidityHigh);
        assertEquals(Long.valueOf(irrigated), pc.get(2).lastIrrigationAt);

        PlotStats s = PlotCards.stats(plots(), crops());
        assertEquals(3, s.count);
        assertEquals(4.2, s.totalAreaMu, 0);
        assertEquals(100, s.utilization);

        // 作物不在地块中：不计入利用率
        List<CropEntity> some = new ArrayList<>(crops());
        some.get(2).plotId = null;
        assertEquals(29, PlotCards.stats(plots(), some).utilization);       // 1.2 / 4.2
        assertEquals(0, PlotCards.stats(Collections.emptyList(), crops()).utilization);
        assertEquals(0, PlotCards.stats(Collections.emptyList(), crops()).count);
    }

    @Test
    public void humidityThreshold() {
        assertTrue(PlotCards.humidityHigh(plot(1, "a", 1, 85)));
        assertFalse(PlotCards.humidityHigh(plot(1, "a", 1, 84)));
        assertFalse(PlotCards.humidityHigh(plot(1, "a", 1, null)));
    }

    @Test
    public void taskItemsAndMonthTypes() {
        TaskRow r = new TaskRow();
        r.id = 5;
        r.title = "检查黄瓜霜霉病";
        r.type = "SPRAY";
        r.cropId = 2L;
        r.cropName = "黄瓜";
        r.plotName = "2号大棚";
        r.dueAt = 1000;
        r.attention = true;
        TaskItem t = TaskItems.from(r);
        assertEquals(FarmType.SPRAY, t.type);
        assertEquals("2号大棚", t.plotName);
        assertTrue(t.attention);

        long d10a = LocalDateTime.of(2026, 10, 10, 9, 0).atZone(ZONE).toInstant().toEpochMilli();
        long d10b = LocalDateTime.of(2026, 10, 10, 16, 0).atZone(ZONE).toInstant().toEpochMilli();
        long d13 = LocalDateTime.of(2026, 10, 13, 23, 59).atZone(ZONE).toInstant().toEpochMilli();
        Map<LocalDate, Set<String>> m = TaskItems.monthTypes(Arrays.asList(
                dayType(d10a, "IRRIGATE"), dayType(d10b, "FERTILIZE"), dayType(d10b, "IRRIGATE"),
                dayType(d13, "BAD")), ZONE);
        assertEquals(2, m.size());
        assertEquals(Arrays.asList("FERTILIZE", "IRRIGATE"),
                new ArrayList<>(m.get(LocalDate.of(2026, 10, 10))));
        assertEquals(Collections.singletonList("OTHER"), new ArrayList<>(m.get(LocalDate.of(2026, 10, 13))));
    }

    private static TaskDayType dayType(long at, String type) {
        TaskDayType t = new TaskDayType();
        t.dueAt = at;
        t.type = type;
        return t;
    }
}
