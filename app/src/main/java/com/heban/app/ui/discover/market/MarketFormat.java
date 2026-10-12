package com.heban.app.ui.discover.market;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.heban.app.data.local.entity.ProductEntity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** D07–D09 共用的纯计算 / 格式化（不依赖 Android，便于单元测试）。界面文字模板在 strings_market.xml。 */
public final class MarketFormat {
    static final long HOUR_MS = 60L * 60 * 1000;

    private MarketFormat() {
    }

    /** 置顶中：pinned_until 晚于当前时间（技术准备文档 12.4） */
    public static boolean isPinned(@Nullable ProductEntity p, long now) {
        return p != null && p.pinnedUntil > now;
    }

    /** 置顶剩余小时数，向上取整（还剩 17.5 小时显示 18）；未置顶为 0 */
    public static int pinnedHoursLeft(long pinnedUntil, long now) {
        long left = pinnedUntil - now;
        if (left <= 0) return 0;
        return (int) ((left + HOUR_MS - 1) / HOUR_MS);
    }

    /** 价格（分）→「25」「1,800」「0.6」「8.5」，不带货币符号 */
    @NonNull
    public static String price(long priceCent) {
        DecimalFormat f = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return f.format(BigDecimal.valueOf(priceCent, 2));
    }

    /** 输入框中的价格（元）→ 分；无法解析或不大于 0 时返回 -1 */
    public static long parsePriceCent(@Nullable String text) {
        if (text == null) return -1;
        String s = text.trim().replace(",", "");
        if (s.isEmpty()) return -1;
        try {
            BigDecimal yuan = new BigDecimal(s);
            if (yuan.signum() <= 0 || yuan.scale() > 2) return -1;
            long cent = yuan.movePointRight(2).longValueExact();
            return cent > 0 ? cent : -1;
        } catch (NumberFormatException | ArithmeticException e) {
            return -1;
        }
    }

    /** 数量 / 距离：10.0 →「10」，1.5 →「1.5」 */
    @NonNull
    public static String number(double value) {
        DecimalFormat f = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return f.format(value);
    }

    /** 积分余额：1280 →「1,280」 */
    @NonNull
    public static String points(int value) {
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        return f.format(value);
    }

    /** 「江苏省 南京市 江宁区」→「江宁区」（D07 江宁区附近）；空时返回 null */
    @Nullable
    public static String district(@Nullable String region) {
        if (region == null) return null;
        String[] parts = region.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? null : last;
    }

    /** 「江苏省 句容市 白兔镇」→「句容白兔镇」（D08 卖家所在地，取后两级并去掉「市」） */
    @NonNull
    public static String shortRegion(@Nullable String region) {
        if (region == null || region.trim().isEmpty()) return "";
        String[] parts = region.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        String city = parts[parts.length - 2];
        if (city.endsWith("市") && city.length() > 2) city = city.substring(0, city.length() - 1);
        return city + parts[parts.length - 1];
    }

    /** 头像文字：昵称首字（UI 设计文档第 7 章） */
    @NonNull
    public static String avatarLetter(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String s = name.trim();
        return s.substring(0, Character.charCount(s.codePointAt(0)));
    }

    /** 商品图片 / 标签字段（JSON 字符串数组）→ 列表；格式错误时返回空列表 */
    @NonNull
    public static List<String> jsonList(@NonNull Gson gson, @Nullable String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            String[] arr = gson.fromJson(json, String[].class);
            if (arr == null) return Collections.emptyList();
            List<String> out = new ArrayList<>();
            for (String s : arr) {
                if (s != null && !s.trim().isEmpty()) out.add(s.trim());
            }
            return out;
        } catch (JsonParseException | IllegalStateException e) {
            return Collections.emptyList();
        }
    }

    /** 列表中的封面（第一张图片），没有时为 null */
    @Nullable
    public static String cover(@NonNull Gson gson, @Nullable ProductEntity p) {
        if (p == null) return null;
        List<String> images = jsonList(gson, p.images);
        return images.isEmpty() ? null : images.get(0);
    }

    /** 搜索：标题、卖家、角标、细分类、描述包含关键字（忽略大小写） */
    public static boolean matches(@Nullable ProductEntity p, @Nullable String keyword) {
        if (p == null) return false;
        if (keyword == null || keyword.trim().isEmpty()) return true;
        String k = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(p.title, k) || contains(p.sellerName, k) || contains(p.tag, k)
                || contains(p.subCategory, k) || contains(p.description, k);
    }

    private static boolean contains(@Nullable String s, String k) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(k);
    }
}
