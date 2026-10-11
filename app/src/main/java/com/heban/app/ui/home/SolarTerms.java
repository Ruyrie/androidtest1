package com.heban.app.ui.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 二十四节气（H03 日历小字与节气提示）。数据在 assets/mock/solar_terms.json：
 * {@code tips} 为每个节气的农事提示，{@code terms} 为各年节气日期（北京时间）。
 */
public final class SolarTerms {
    public static final String ASSET = "mock/solar_terms.json";

    /** 一个节气：日期、名称、农事提示（可能为空字符串） */
    public static final class Term {
        @NonNull public final LocalDate date;
        @NonNull public final String name;
        @NonNull public final String tip;

        Term(@NonNull LocalDate date, @NonNull String name, @NonNull String tip) {
            this.date = date;
            this.name = name;
            this.tip = tip;
        }
    }

    /** JSON 结构 */
    static final class Json {
        Map<String, String> tips;
        List<Entry> terms;
    }

    static final class Entry {
        String date;
        String name;
    }

    public static final SolarTerms EMPTY = new SolarTerms(new TreeMap<>());

    private static volatile SolarTerms cached;

    private final NavigableMap<LocalDate, Term> byDate;

    private SolarTerms(NavigableMap<LocalDate, Term> byDate) {
        this.byDate = byDate;
    }

    /** 读取并缓存 assets 中的节气表（文件损坏时返回 EMPTY，日历照常显示） */
    @NonNull
    public static SolarTerms load(@NonNull Context context) {
        SolarTerms s = cached;
        if (s != null) return s;
        synchronized (SolarTerms.class) {
            if (cached == null) {
                try (InputStream in = context.getAssets().open(ASSET)) {
                    cached = parse(new InputStreamReader(in, StandardCharsets.UTF_8));
                } catch (IOException | RuntimeException e) {
                    cached = EMPTY;
                }
            }
            return cached;
        }
    }

    /** 解析节气 JSON；日期或名称缺失的条目跳过 */
    @NonNull
    public static SolarTerms parse(@NonNull Reader reader) {
        Json json;
        try {
            json = new Gson().fromJson(reader, Json.class);
        } catch (JsonParseException e) {
            return EMPTY;
        }
        if (json == null || json.terms == null) return EMPTY;
        Map<String, String> tips = json.tips == null ? Collections.emptyMap() : json.tips;
        NavigableMap<LocalDate, Term> map = new TreeMap<>();
        for (Entry e : json.terms) {
            if (e == null || e.date == null || e.name == null || e.name.isEmpty()) continue;
            try {
                LocalDate d = LocalDate.parse(e.date);
                String tip = tips.get(e.name);
                map.put(d, new Term(d, e.name, tip == null ? "" : tip));
            } catch (DateTimeParseException ignored) {
                // 跳过格式错误的日期
            }
        }
        return new SolarTerms(map);
    }

    /** 这一天的节气名称，不是节气时为 null */
    @Nullable
    public String nameOn(@NonNull LocalDate day) {
        Term t = byDate.get(day);
        return t == null ? null : t.name;
    }

    /** day 当天或之后最近的节气；超出数据范围时为 null */
    @Nullable
    public Term nextOnOrAfter(@NonNull LocalDate day) {
        Map.Entry<LocalDate, Term> e = byDate.ceilingEntry(day);
        return e == null ? null : e.getValue();
    }

    public int size() {
        return byDate.size();
    }

    /** 某一年的全部节气（按日期排序） */
    @NonNull
    public List<Term> inYear(int year) {
        return Collections.unmodifiableList(new ArrayList<>(
                byDate.subMap(LocalDate.of(year, 1, 1), true, LocalDate.of(year, 12, 31), true).values()));
    }
}
