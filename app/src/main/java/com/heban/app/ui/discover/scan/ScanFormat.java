package com.heban.app.ui.discover.scan;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * D02 / D03 的纯逻辑：匹配度、严重程度配色、特征标签、症状说明、「问 AI」提示词（技术准备文档 9.4）、
 * 「发到问答」文字、保存目标作物匹配与识别提示作物。不依赖 Android 视图，便于单元测试。
 */
public final class ScanFormat {
    /** 健康植株：top.category 写「健康」（技术准备文档 9.5） */
    public static final String CATEGORY_HEALTHY = "健康";
    /** urgency_days 不超过该天数时显示「传播快」标签 */
    public static final int FAST_SPREAD_DAYS = 3;
    /** 识别提示最多带几种作物 */
    static final int MAX_HINT_CROPS = 5;

    /** 严重程度标签配色：轻度 Tertiary，中度 Warning，重度 Error，健康 Primary Container */
    public enum Severity { NONE, MILD, MODERATE, SEVERE, HEALTHY }

    private ScanFormat() {
    }

    /** 置信度 0–1 → 百分比 0–100（四舍五入） */
    public static int percent(double confidence) {
        if (Double.isNaN(confidence)) return 0;
        double c = Math.max(0, Math.min(1, confidence));
        return (int) Math.round(c * 100);
    }

    /** 「其他可能」小进度条的长度：与原型一致按 2 倍百分比显示（6% → 12%），最多 100 */
    public static int otherBarProgress(double confidence) {
        return Math.min(100, percent(confidence) * 2);
    }

    public static boolean isHealthy(@Nullable ScanResult.Top top) {
        return top != null && top.category != null && top.category.trim().equals(CATEGORY_HEALTHY);
    }

    @NonNull
    public static Severity severity(@Nullable ScanResult.Top top) {
        if (top == null) return Severity.NONE;
        if (isHealthy(top)) return Severity.HEALTHY;
        String s = top.severity == null ? "" : top.severity.trim();
        switch (s) {
            case "轻度":
                return Severity.MILD;
            case "中度":
                return Severity.MODERATE;
            case "重度":
            case "严重":
                return Severity.SEVERE;
            default:
                return Severity.NONE;
        }
    }

    /** 严重程度标签的背景 / 文字颜色属性：{container, onContainer}；NONE 不显示标签，返回 null */
    @Nullable
    @AttrRes
    public static int[] severityColors(@NonNull Severity s) {
        switch (s) {
            case MILD:
                return new int[]{com.google.android.material.R.attr.colorTertiaryContainer,
                        com.google.android.material.R.attr.colorOnTertiaryContainer};
            case MODERATE:
                return new int[]{R.attr.colorWarningContainer, R.attr.colorOnWarningContainer};
            case SEVERE:
                return new int[]{com.google.android.material.R.attr.colorErrorContainer,
                        com.google.android.material.R.attr.colorOnErrorContainer};
            case HEALTHY:
                return new int[]{com.google.android.material.R.attr.colorPrimaryContainer,
                        com.google.android.material.R.attr.colorOnPrimaryContainer};
            default:
                return null;
        }
    }

    /** 严重程度标签文字：健康显示「健康」，其余为模型返回的轻度 / 中度 / 重度 */
    @NonNull
    public static String severityLabel(@Nullable ScanResult.Top top) {
        Severity s = severity(top);
        if (s == Severity.NONE) return "";
        if (s == Severity.HEALTHY) return CATEGORY_HEALTHY;
        return top.severity.trim();
    }

    /** 是否「传播快」：urgency_days 在 1–3 天内（健康植株不显示） */
    public static boolean spreadsFast(@Nullable ScanResult.Top top) {
        return top != null && !isHealthy(top) && top.urgencyDays > 0 && top.urgencyDays <= FAST_SPREAD_DAYS;
    }

    /** 特征标签：病害类型、发病部位，必要时追加 extraTag（「传播快」） */
    @NonNull
    public static List<String> tags(@Nullable ScanResult.Top top, @NonNull String fastTag) {
        if (top == null) return Collections.emptyList();
        Set<String> out = new LinkedHashSet<>();
        if (notBlank(top.category)) out.add(top.category.trim());
        if (notBlank(top.part)) out.add(top.part.trim());
        if (spreadsFast(top)) out.add(fastTag);
        return new ArrayList<>(out);
    }

    /**
     * 症状说明：判断依据 + 处理时限，例如
     * 「叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层，与霜霉病典型症状一致。建议 3 天内处理，防止扩散到整棚。」
     */
    @NonNull
    public static String evidenceText(@Nullable ScanResult.Top top) {
        if (top == null) return "";
        StringBuilder sb = new StringBuilder();
        if (notBlank(top.evidence)) sb.append(stripEnd(top.evidence.trim())).append('。');
        if (!isHealthy(top) && top.urgencyDays > 0) {
            sb.append("建议 ").append(top.urgencyDays).append(" 天内处理");
            sb.append(spreadsFast(top) ? "，防止扩散到整棚。" : "。");
        }
        return sb.toString();
    }

    /**
     * D03「问 AI」带入的第一条消息（技术准备文档 9.4）：
     * 「拍照识别结果：黄瓜霜霉病（中度，匹配度 92%），依据：……。请给出详细防治方案。」
     */
    @NonNull
    public static String aiPrompt(@Nullable ScanResult result) {
        ScanResult.Top top = result == null ? null : result.top;
        if (top == null) return "";
        StringBuilder sb = new StringBuilder("拍照识别结果：").append(nameOf(top)).append('（');
        Severity s = severity(top);
        if (s != Severity.NONE && s != Severity.HEALTHY) sb.append(top.severity.trim()).append('，');
        if (s == Severity.HEALTHY) sb.append(CATEGORY_HEALTHY).append('，');
        sb.append("匹配度 ").append(percent(top.confidence)).append("%）");
        if (notBlank(top.evidence)) sb.append("，依据：").append(stripEnd(top.evidence.trim()));
        sb.append('。');
        sb.append(s == Severity.HEALTHY ? "请给出日常管理建议。" : "请给出详细防治方案。");
        return sb.toString();
    }

    /** D03「发到问答」预填的问题，例如「这是黄瓜霜霉病吗？拍照识别匹配度 92%，症状：……。请农友帮忙看看。」 */
    @NonNull
    public static String publishText(@Nullable ScanResult result) {
        ScanResult.Top top = result == null ? null : result.top;
        if (top == null) return "";
        StringBuilder sb = new StringBuilder();
        if (isHealthy(top)) {
            String crop = notBlank(result.crop) ? result.crop.trim() : "作物";
            sb.append("这株").append(crop).append("健康吗？");
        } else {
            sb.append("这是").append(nameOf(top)).append("吗？");
        }
        sb.append("拍照识别匹配度 ").append(percent(top.confidence)).append('%');
        if (notBlank(top.evidence)) sb.append("，症状：").append(stripEnd(top.evidence.trim()));
        sb.append("。请农友帮忙看看。");
        return sb.toString();
    }

    /** 历史记录标题：照片不可用 → unusable，否则为 top.name */
    @NonNull
    public static String historyTitle(@Nullable ScanResult result, @NonNull String unusable) {
        if (result == null || !result.imageOk || result.top == null || !notBlank(result.top.name)) return unusable;
        return result.top.name.trim();
    }

    @Nullable
    public static ScanResult parse(@NonNull Gson gson, @Nullable String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return gson.fromJson(json, ScanResult.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 「黄瓜 · 2号大棚」；没有地块时只显示作物名 */
    @NonNull
    public static String cropLabel(@NonNull CropCard card) {
        String name = card.name == null ? "" : card.name;
        if (!notBlank(card.plotName)) return name;
        return name + " · " + card.plotName.trim();
    }

    /** 名称与识别出的作物（result.crop）相同或互相包含的作物，名称完全相同的排在前面 */
    @NonNull
    public static List<CropCard> matchingCrops(@Nullable List<CropCard> crops, @Nullable String crop) {
        if (crops == null || !notBlank(crop)) return Collections.emptyList();
        String c = crop.trim();
        List<CropCard> exact = new ArrayList<>();
        List<CropCard> partial = new ArrayList<>();
        for (CropCard card : crops) {
            if (card == null || !notBlank(card.name)) continue;
            String n = card.name.trim();
            if (n.equals(c)) exact.add(card);
            else if (n.contains(c) || c.contains(n)) partial.add(card);
        }
        exact.addAll(partial);
        return exact;
    }

    /** 识别提示：用户在种的作物名称（去重，最多 5 种，用顿号连接），没有作物时为 null */
    @Nullable
    public static String cropHint(@Nullable List<CropCard> crops) {
        if (crops == null || crops.isEmpty()) return null;
        Set<String> names = new LinkedHashSet<>();
        for (CropCard card : crops) {
            if (card != null && notBlank(card.name)) names.add(card.name.trim());
            if (names.size() >= MAX_HINT_CROPS) break;
        }
        return names.isEmpty() ? null : String.join("、", names);
    }

    @NonNull
    private static String nameOf(@NonNull ScanResult.Top top) {
        return top.name == null ? "" : top.name.trim();
    }

    private static boolean notBlank(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** 去掉结尾的标点（句号、逗号等），便于拼接 */
    @NonNull
    static String stripEnd(@NonNull String s) {
        int end = s.length();
        while (end > 0 && "。，、；;,.！!？? ".indexOf(s.charAt(end - 1)) >= 0) end--;
        return s.substring(0, end);
    }
}
