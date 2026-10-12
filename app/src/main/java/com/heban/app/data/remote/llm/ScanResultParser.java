package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.heban.app.data.model.ScanResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析识图模型返回的 JSON（技术准备文档 9.5 / 表 9-3）：先截取第一个「{」到最后一个「}」之间的内容再交给 Gson
 * （兼容模型偶尔加代码块标记或多余文字），并检查 confidence 是否在 0–1 之间。
 * 解析失败抛出 {@link ScanParseException}（视为该模型失败，触发备用模型）。
 */
public final class ScanResultParser {

    /** 返回内容不是合法 JSON 或字段缺失 */
    public static final class ScanParseException extends JsonParseException {
        public ScanParseException(String msg) {
            super(msg);
        }

        public ScanParseException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    private ScanResultParser() {
    }

    @NonNull
    public static ScanResult parse(@Nullable String text, @NonNull Gson gson) {
        if (text == null) throw new ScanParseException("模型没有返回内容");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) throw new ScanParseException("返回内容中没有 JSON 对象");
        ScanResult r;
        try {
            r = gson.fromJson(text.substring(start, end + 1), ScanResult.class);
        } catch (JsonParseException | NumberFormatException e) {
            throw new ScanParseException("返回的 JSON 无法解析", e);
        }
        if (r == null) throw new ScanParseException("返回的 JSON 为空");
        return validate(r);
    }

    /** 检查必填字段与置信度范围，并把空列表 / 空字符串规范化 */
    @NonNull
    static ScanResult validate(@NonNull ScanResult r) {
        r.crop = trim(r.crop);
        r.imageIssue = trim(r.imageIssue);
        if (r.top != null) {
            ScanResult.Top t = r.top;
            t.name = trim(t.name);
            t.category = trim(t.category);
            t.part = trim(t.part);
            t.severity = trim(t.severity);
            t.evidence = trim(t.evidence);
            List<String> advice = new ArrayList<>();
            if (t.advice != null) {
                for (String a : t.advice) {
                    if (a != null && !a.trim().isEmpty()) advice.add(a.trim());
                }
            }
            t.advice = advice;
            if (t.urgencyDays < 0) t.urgencyDays = 0;
        }
        if (r.imageOk) {
            if (r.top == null) throw new ScanParseException("缺少 top 字段");
            if (r.top.name.isEmpty()) throw new ScanParseException("缺少 top.name");
            if (!inRange(r.top.confidence)) throw new ScanParseException("top.confidence 不在 0–1 之间");
        } else if (r.top != null && (r.top.name.isEmpty() || !inRange(r.top.confidence))) {
            r.top = null;   // 照片不可用时 top 可有可无
        }
        List<ScanResult.Other> others = new ArrayList<>();
        if (r.others != null) {
            for (ScanResult.Other o : r.others) {
                if (o == null || o.name == null || o.name.trim().isEmpty()) continue;
                if (!inRange(o.confidence)) throw new ScanParseException("others.confidence 不在 0–1 之间");
                o.name = o.name.trim();
                others.add(o);
            }
        }
        r.others = others;
        return r;
    }

    static boolean inRange(double v) {
        return !Double.isNaN(v) && v >= 0 && v <= 1;
    }

    @NonNull
    private static String trim(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}
