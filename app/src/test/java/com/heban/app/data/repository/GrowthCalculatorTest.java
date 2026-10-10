package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.StageInfo;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class GrowthCalculatorTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static long day(int y, int m, int d) {
        return DateRanges.startOfDay(LocalDate.of(y, m, d), ZONE);
    }

    @Test
    public void tomatoPrototypeNumbers() throws Exception {
        // C02：定植 08/03，已种 68 天，预计 11/21 采收，预计还需 42 天，当前「结果」
        CropProfile tomato = ProfileFixture.profile("番茄");
        long planted = day(2026, 8, 3);
        int days = GrowthCalculator.daysPlanted(planted, LocalDate.of(2026, 10, 9), ZONE);
        assertEquals(68, days);
        assertEquals(110, tomato.expectedDays);
        assertEquals(61, GrowthCalculator.progress(days, 110));
        assertEquals("结果期", GrowthCalculator.stage(tomato, days, 110));
        assertEquals("结果", GrowthCalculator.stageName(tomato, days, 110));
        assertEquals(42, GrowthCalculator.daysLeft(days, 110));
        assertEquals(day(2026, 11, 21), GrowthCalculator.expectedHarvestAt(planted, 110, ZONE));

        List<StageInfo> stages = GrowthCalculator.stageInfos(tomato, days, 110);
        String[] names = {"育苗", "定植", "开花", "结果", "采收"};
        assertEquals(names.length, stages.size());
        for (int i = 0; i < names.length; i++) {
            assertEquals(names[i], stages.get(i).name);
            assertEquals(i < 3, stages.get(i).done);
            assertEquals(i == 3, stages.get(i).current);
        }
    }

    @Test
    public void otherPrototypeCrops() throws Exception {
        assertEquals("开花期", GrowthCalculator.stage(ProfileFixture.profile("黄瓜"), 35, 78));
        assertEquals(78, ProfileFixture.profile("黄瓜").expectedDays);
        assertEquals("分蘖期", GrowthCalculator.stage(ProfileFixture.profile("冬小麦"), 21, 240));
        assertEquals(240, ProfileFixture.profile("冬小麦").expectedDays);
        assertEquals("定植期", GrowthCalculator.stage(ProfileFixture.profile("草莓"), 3, 150));
        assertEquals(150, ProfileFixture.profile("草莓").expectedDays);
        assertEquals("结果期", GrowthCalculator.stage(ProfileFixture.profile("樱桃番茄"), 68, 110));
        // C01 进度条：35/78、21/240、3/150
        assertEquals(44, GrowthCalculator.progress(35, 78));
        assertEquals(8, GrowthCalculator.progress(21, 240));
        assertEquals(2, GrowthCalculator.progress(3, 150));
    }

    @Test
    public void plantedDayIsDayOne() {
        long planted = day(2026, 10, 10);
        assertEquals(1, GrowthCalculator.daysPlanted(planted, LocalDate.of(2026, 10, 10), ZONE));
        assertEquals(2, GrowthCalculator.daysPlanted(planted + 15 * 3600_000L, LocalDate.of(2026, 10, 11), ZONE));
        // 定植日在未来
        assertEquals(0, GrowthCalculator.daysPlanted(planted, LocalDate.of(2026, 10, 8), ZONE));
    }

    @Test
    public void progressAndDaysLeftBounds() {
        assertEquals(100, GrowthCalculator.progress(200, 110));
        assertEquals(0, GrowthCalculator.progress(0, 110));
        assertEquals(0, GrowthCalculator.progress(10, 0));
        assertEquals(0, GrowthCalculator.daysLeft(200, 110));
    }

    @Test
    public void stageBoundariesAndEnd() throws Exception {
        CropProfile tomato = ProfileFixture.profile("番茄");
        assertEquals("定植期", GrowthCalculator.stage(tomato, 0, 110));
        assertEquals("定植期", GrowthCalculator.stage(tomato, 1, 110));
        assertEquals("开花期", GrowthCalculator.stage(tomato, 25, 110));
        List<StageInfo> end = GrowthCalculator.stageInfos(tomato, 500, 110);
        StageInfo last = end.get(end.size() - 1);
        assertTrue(last.current);
        assertFalse(last.done);
    }

    @Test
    public void customExpectedDaysScalesStages() throws Exception {
        CropProfile tomato = ProfileFixture.profile("番茄");
        // 预计天数缩短一半：结果期从第 45 天提前到第 23 天左右
        assertEquals("结果期", GrowthCalculator.stage(tomato, 30, 55));
        assertEquals("开花期", GrowthCalculator.stage(tomato, 30, 110));
    }

    @Test
    public void stageLabel() {
        assertEquals("结果期", GrowthCalculator.stageLabel("结果"));
        assertEquals("苗期", GrowthCalculator.stageLabel("苗期"));
        assertNull(GrowthCalculator.stage(new CropProfile(), 3, 90));
    }

    @Test
    public void defaultProfileStages() {
        CropProfile p = CropProfiles.defaultProfile("不知名作物");
        assertEquals("定植期", GrowthCalculator.stage(p, 1, 90));
        assertEquals("生长期", GrowthCalculator.stage(p, 30, 90));
        assertEquals("成熟期", GrowthCalculator.stage(p, 80, 90));
    }
}
