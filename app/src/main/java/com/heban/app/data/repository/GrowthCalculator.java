package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.StageInfo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 生长进度计算（C01 / C02 / H01，纯 Java）：
 * <ul>
 *   <li>已种天数 = 定植日到今天的天数 + 1（定植当天为第 1 天），定植日在未来时为 0；</li>
 *   <li>进度 = min(100, 已种天数 × 100 ÷ 预计天数)；</li>
 *   <li>当前阶段 = 生长期表中 startDay ≤ 已种天数 的最后一个阶段（预计天数与生长期表不同时按比例缩放 startDay）；</li>
 *   <li>阶段名称保持生长期表原样（C02 步进条「育苗 / 定植 / 开花 / 结果 / 采收」），
 *       卡片上显示加「期」的写法（「结果期」），见 {@link #stageLabel(String)}；</li>
 *   <li>预计还需 = max(0, 预计天数 − 已种天数)，预计采收 = 定植日 + 预计天数。</li>
 * </ul>
 */
public final class GrowthCalculator {
    private GrowthCalculator() {
    }

    /** 已种天数（定植当天为第 1 天；定植日在今天之后返回 0） */
    public static int daysPlanted(long plantedAt, LocalDate today, ZoneId zone) {
        LocalDate planted = DateRanges.toDate(plantedAt, zone);
        long days = ChronoUnit.DAYS.between(planted, today) + 1;
        if (days < 0) return 0;
        return days > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) days;
    }

    /** 生长进度 0–100 */
    public static int progress(int daysPlanted, int expectedDays) {
        if (expectedDays <= 0 || daysPlanted <= 0) return 0;
        long p = (long) daysPlanted * 100 / expectedDays;
        return (int) Math.min(100, p);
    }

    /** 距预计采收还需多少天（≥ 0） */
    public static int daysLeft(int daysPlanted, int expectedDays) {
        return Math.max(0, expectedDays - daysPlanted);
    }

    /** 预计采收日期 = 定植日 + 预计天数（当天 0 点毫秒） */
    public static long expectedHarvestAt(long plantedAt, int expectedDays, ZoneId zone) {
        LocalDate planted = DateRanges.toDate(plantedAt, zone);
        return DateRanges.startOfDay(planted.plusDays(Math.max(0, expectedDays)), zone);
    }

    /**
     * 阶段开始天数。作物的预计天数与生长期表不同时，定植后的阶段按比例缩放（育苗等负数不变）。
     */
    static int scaledStart(int startDay, int profileDays, int expectedDays) {
        if (startDay <= 0 || profileDays <= 0 || expectedDays <= 0 || profileDays == expectedDays) {
            return startDay;
        }
        return (int) Math.round(startDay * (double) expectedDays / profileDays);
    }

    /** 当前阶段在 stages 中的下标；生长期表没有阶段时返回 -1 */
    public static int currentStageIndex(CropProfile profile, int daysPlanted, int expectedDays) {
        List<CropProfile.Stage> stages = stagesOf(profile);
        if (stages.isEmpty()) return -1;
        int current = 0;
        for (int i = 0; i < stages.size(); i++) {
            CropProfile.Stage s = stages.get(i);
            if (scaledStart(s.startDay, profile.expectedDays, expectedDays) <= daysPlanted) {
                current = i;
            } else {
                break;
            }
        }
        return current;
    }

    /** 当前阶段名称（生长期表原样，如「结果」）；没有阶段时为 null */
    @Nullable
    public static String stageName(CropProfile profile, int daysPlanted, int expectedDays) {
        int i = currentStageIndex(profile, daysPlanted, expectedDays);
        return i < 0 ? null : stagesOf(profile).get(i).name;
    }

    /** 卡片上显示的阶段（如「结果期」）；没有阶段时为 null */
    @Nullable
    public static String stage(CropProfile profile, int daysPlanted, int expectedDays) {
        String name = stageName(profile, daysPlanted, expectedDays);
        return name == null ? null : stageLabel(name);
    }

    /** 「结果」→「结果期」；已经以「期」结尾的保持不变 */
    public static String stageLabel(String stageName) {
        if (stageName == null || stageName.isEmpty()) return stageName;
        return stageName.endsWith("期") ? stageName : stageName + "期";
    }

    /** C02 步进条：当前阶段之前的为已完成 */
    public static List<StageInfo> stageInfos(CropProfile profile, int daysPlanted, int expectedDays) {
        List<CropProfile.Stage> stages = stagesOf(profile);
        int current = currentStageIndex(profile, daysPlanted, expectedDays);
        List<StageInfo> out = new ArrayList<>(stages.size());
        for (int i = 0; i < stages.size(); i++) {
            out.add(new StageInfo(stages.get(i).name, i < current, i == current));
        }
        return out;
    }

    private static List<CropProfile.Stage> stagesOf(CropProfile profile) {
        return profile == null || profile.stages == null
                ? Collections.<CropProfile.Stage>emptyList() : profile.stages;
    }
}
