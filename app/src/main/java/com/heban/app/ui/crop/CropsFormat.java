package com.heban.app.ui.crop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.CropCard;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 作物、地块页面共用的文字格式（面积、金额、日期、卡片副标题等），与原型写法一致。 */
public final class CropsFormat {
    /** 原型中的分隔符「 · 」 */
    public static final String SEP = " · ";
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM/dd", Locale.CHINA);
    private static final DateTimeFormatter MONTH_DAY_CN = DateTimeFormatter.ofPattern("MM月dd日", Locale.CHINA);

    private CropsFormat() {
    }

    /** 面积：至少一位小数，最多两位（3 → 3.0，0.25 → 0.25） */
    @NonNull
    public static String area(double mu) {
        return new DecimalFormat("0.0#", DecimalFormatSymbols.getInstance(Locale.US)).format(mu);
    }

    /** 「0.5 亩」 */
    @NonNull
    public static String areaMu(double mu) {
        return area(mu) + " 亩";
    }

    /** 分 → 「¥1,280」「¥12.5」 */
    @NonNull
    public static String money(long cent) {
        DecimalFormat f = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return "¥" + f.format(cent / 100.0);
    }

    /** 温度：整数不带小数（22），否则一位小数（22.5） */
    @NonNull
    public static String temperature(double celsius) {
        return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US)).format(celsius);
    }

    /** 数量：去掉多余的 0（5.0 → 5，2.5 → 2.5） */
    @NonNull
    public static String amount(double v) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(v);
    }

    /** 「08/03」（C02 定植、采收、农事记录日期） */
    @NonNull
    public static String monthDay(long millis, ZoneId zone) {
        return MONTH_DAY.format(Instant.ofEpochMilli(millis).atZone(zone));
    }

    /** 「10月05日」（C05 上次灌溉） */
    @NonNull
    public static String monthDayCn(long millis, ZoneId zone) {
        return MONTH_DAY_CN.format(Instant.ofEpochMilli(millis).atZone(zone));
    }

    /** 用「 · 」连接非空片段 */
    @NonNull
    public static String join(@Nullable String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(SEP);
            sb.append(p.trim());
        }
        return sb.toString();
    }

    /** 卡片标题「番茄 · 樱桃番茄」 */
    @NonNull
    public static String cropTitle(@Nullable String name, @Nullable String variety) {
        return join(name, variety);
    }

    /**
     * C01 卡片第二行：地块 · 面积，「需关注」标签占用阶段位置时补上阶段，
     * 地块名称看不出种植方式时补上方式（原型「东地块 · 3.0 亩 · 露天」「2号大棚 · 0.4 亩 · 开花期」）。
     */
    @NonNull
    public static String cropSubtitle(@NonNull CropCard c) {
        String stage = hasAttention(c) ? c.stage : null;
        String mode = c.plantMode;
        if (mode != null && c.plotName != null && c.plotName.contains(mode)) mode = null;
        return join(c.plotName, areaMu(c.areaMu), stage, mode);
    }

    public static boolean hasAttention(@NonNull CropCard c) {
        return c.attentionNote != null && !c.attentionNote.trim().isEmpty();
    }

    /** C02 副标题「樱桃番茄 · 1号大棚 · 0.5 亩」 */
    @NonNull
    public static String detailSubtitle(@Nullable String variety, @Nullable String plotName, double areaMu) {
        return join(variety, plotName, areaMu(areaMu));
    }

    /** 步进条节点名：去掉末尾的「期」（结果期 → 结果），与原型一致 */
    @NonNull
    public static String stepLabel(@Nullable String stage) {
        if (stage == null) return "";
        String s = stage.trim();
        if (s.length() > 2 && s.endsWith("期")) return s.substring(0, s.length() - 1);
        return s;
    }

    /** 搜索：名称、品种、地块包含关键字（忽略大小写） */
    @NonNull
    public static List<CropCard> search(@Nullable List<CropCard> cards, @Nullable String query) {
        if (cards == null) return Collections.emptyList();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return cards;
        List<CropCard> out = new ArrayList<>();
        for (CropCard c : cards) {
            if (contains(c.name, q) || contains(c.variety, q) || contains(c.plotName, q)) out.add(c);
        }
        return out;
    }

    private static boolean contains(@Nullable String text, String q) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(q);
    }

    /** 农事记录副标题：物料 用量 单位 · 金额 · 备注 */
    @NonNull
    public static String recordSubtitle(@NonNull FarmRecordEntity r) {
        String qty = r.amount > 0 ? join2(amount(r.amount), r.unit) : null;
        String main = join2(r.material, qty);
        String cost = r.costCent > 0 ? money(r.costCent) : null;
        return join(main, cost, r.note);
    }

    /** 空格连接 */
    @Nullable
    private static String join2(@Nullable String a, @Nullable String b) {
        boolean ea = a == null || a.trim().isEmpty();
        boolean eb = b == null || b.trim().isEmpty();
        if (ea && eb) return null;
        if (ea) return b.trim();
        if (eb) return a.trim();
        return a.trim() + " " + b.trim();
    }

    /** FarmRecordEntity.photos（JSON 字符串数组）→ 路径列表，格式不对时返回空列表 */
    @NonNull
    public static List<String> parsePhotos(@NonNull Gson gson, @Nullable String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            String[] arr = gson.fromJson(json, String[].class);
            if (arr == null) return Collections.emptyList();
            List<String> out = new ArrayList<>();
            for (String s : arr) {
                if (s != null && !s.trim().isEmpty()) out.add(s);
            }
            return out;
        } catch (JsonParseException | IllegalStateException e) {
            return Collections.emptyList();
        }
    }
}
