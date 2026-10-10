package com.heban.app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.model.CropProfile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 作物生长期表（assets/mock/crop_profiles.json）：首次使用时读取并缓存。
 * 匹配规则：名称完全相同 → 名称包含表中作物名（取最长的，如「樱桃番茄苗」→ 樱桃番茄）
 * → 表中作物名包含输入（如「小麦」→ 冬小麦）→ 通用默认值（90 天、eco 图标、定植 / 生长 / 成熟）。
 */
public final class CropProfiles {
    public static final String ASSET = "mock/crop_profiles.json";
    public static final int DEFAULT_DAYS = 90;
    public static final String DEFAULT_ICON = "eco";
    private static final String TAG = "CropProfiles";

    private final Context app;
    private final Gson gson;
    private volatile List<CropProfile> cache;

    public CropProfiles(Context app, Gson gson) {
        this.app = app;
        this.gson = gson;
    }

    /** 全部作物（JSON 中的顺序）；读取失败时为空列表 */
    public List<CropProfile> all() {
        List<CropProfile> list = cache;
        if (list == null) {
            synchronized (this) {
                list = cache;
                if (list == null) {
                    list = load();
                    cache = list;
                }
            }
        }
        return list;
    }

    /** 按作物名称匹配 */
    public CropProfile forName(@Nullable String name) {
        return match(all(), name);
    }

    /** 按作物名称和品种匹配：品种与表中某种作物完全相同时优先（如 番茄 · 樱桃番茄） */
    public CropProfile forCrop(@Nullable String name, @Nullable String variety) {
        return match(all(), name, variety);
    }

    private List<CropProfile> load() {
        try (Reader r = new InputStreamReader(app.getAssets().open(ASSET), StandardCharsets.UTF_8)) {
            return parse(gson, r);
        } catch (Exception e) {
            Log.w(TAG, "crop profiles unavailable", e);
            return Collections.emptyList();
        }
    }

    // —— 以下为纯 Java 逻辑 ——

    public static List<CropProfile> parse(Gson gson, Reader reader) {
        CropProfile[] arr = gson.fromJson(reader, CropProfile[].class);
        if (arr == null) return Collections.emptyList();
        List<CropProfile> out = new ArrayList<>(Arrays.asList(arr));
        for (CropProfile p : out) {
            if (p.stages == null) p.stages = new ArrayList<>();
        }
        return Collections.unmodifiableList(out);
    }

    public static CropProfile match(List<CropProfile> all, @Nullable String name, @Nullable String variety) {
        String v = variety == null ? "" : variety.trim();
        if (!v.isEmpty()) {
            CropProfile byVariety = exact(all, v);
            if (byVariety != null) return byVariety;
        }
        return match(all, name);
    }

    public static CropProfile match(List<CropProfile> all, @Nullable String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) return defaultProfile(n);
        CropProfile p = exact(all, n);
        if (p != null) return p;
        // 输入包含表中作物名：取最长的作物名
        CropProfile best = null;
        for (CropProfile c : all) {
            if (c.name != null && !c.name.isEmpty() && n.contains(c.name)
                    && (best == null || c.name.length() > best.name.length())) {
                best = c;
            }
        }
        if (best != null) return best;
        // 表中作物名包含输入（至少 2 个字，避免「瓜」这类单字误配）
        if (n.length() >= 2) {
            for (CropProfile c : all) {
                if (c.name != null && c.name.contains(n)) return c;
            }
        }
        return defaultProfile(n);
    }

    @Nullable
    private static CropProfile exact(List<CropProfile> all, String name) {
        for (CropProfile c : all) {
            if (name.equals(c.name)) return c;
        }
        return null;
    }

    /** 通用默认值：90 天，图标 eco，阶段 定植 / 生长 / 成熟（卡片显示为 定植期 / 生长期 / 成熟期），无示例图片 */
    public static CropProfile defaultProfile(@Nullable String name) {
        CropProfile p = new CropProfile();
        p.name = name == null ? "" : name;
        p.expectedDays = DEFAULT_DAYS;
        p.icon = DEFAULT_ICON;
        p.image = null;
        p.stages.add(stage("定植", 0));
        p.stages.add(stage("生长", 15));
        p.stages.add(stage("成熟", 75));
        return p;
    }

    private static CropProfile.Stage stage(String name, int startDay) {
        CropProfile.Stage s = new CropProfile.Stage();
        s.name = name;
        s.startDay = startDay;
        return s;
    }
}
