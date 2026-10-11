package com.heban.app.ui.crop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.heban.app.R;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * C03 表单的纯计算：预计采收日期 = 定植日期 + 作物默认生长期（技术准备文档 12.3），
 * 采收时间按上旬 / 中旬 / 下旬显示；日期与毫秒互转；面积等小数输入的解析。
 */
public final class AddCropHarvest {
    private AddCropHarvest() {
    }

    /** 预计采收日期；生长期未知（≤ 0）时返回 null */
    @Nullable
    public static LocalDate expectedHarvest(@Nullable LocalDate planted, int expectedDays) {
        if (planted == null || expectedDays <= 0) return null;
        return planted.plusDays(expectedDays);
    }

    /** 1–10 日为上旬，11–20 日为中旬，其余为下旬 */
    @StringRes
    public static int periodRes(@NonNull LocalDate date) {
        int d = date.getDayOfMonth();
        if (d <= 10) return R.string.add_crop_period_early;
        if (d <= 20) return R.string.add_crop_period_middle;
        return R.string.add_crop_period_late;
    }

    /** 定植日期存为当天 0 点（本地时区）的毫秒数 */
    public static long startOfDayMillis(@NonNull LocalDate date, @NonNull ZoneId zone) {
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    @NonNull
    public static LocalDate toLocalDate(long millis, @NonNull ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
    }

    /** MaterialDatePicker 的选择值是 UTC 当天 0 点 */
    public static long toPickerSelection(@NonNull LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @NonNull
    public static LocalDate fromPickerSelection(long utcMillis) {
        return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    /**
     * 解析非负小数输入（面积、用量）：空白返回 null；格式不对（如「.」「1.2.3」）返回 NaN。
     */
    @Nullable
    public static Double parseDecimal(@Nullable String text) {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) return null;
        if (!s.matches("\\d+(\\.\\d*)?|\\.\\d+")) return Double.NaN;
        return new BigDecimal(s).doubleValue();
    }

    /** 小数回填到输入框：去掉多余的 0（0.30 → 0.3，2.0 → 2） */
    @NonNull
    public static String formatDecimal(double value) {
        if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) return "";
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
