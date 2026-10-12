package com.heban.app.ui.discover.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * D06 回答文字的轻量 Markdown 解析（纯 Java，便于单元测试）：按行拆成段落、标题、有序 / 无序列表，
 * 行内只处理「**粗体**」。流式输出时文字可能停在一半，未闭合的「**」按粗体处理到行尾。
 * 排版见 {@link AiChatSpans}：第一段为正文大字，其余段落为说明小字（原型 AIChat.dc.html）。
 */
public final class AiChatMarkdown {
    public enum Kind { PARAGRAPH, HEADING, ORDERED, BULLET }

    /** 行内片段 */
    public static final class Run {
        @NonNull public final String text;
        public final boolean bold;

        Run(@NonNull String text, boolean bold) {
            this.text = text;
            this.bold = bold;
        }
    }

    /** 一个块（一行） */
    public static final class Block {
        @NonNull public final Kind kind;
        /** 列表序号「1.」或「•」，其他为 null */
        @Nullable public final String marker;
        @NonNull public final List<Run> runs;
        /** 是否为回答开头的第一段（正文大字） */
        public final boolean lead;

        Block(@NonNull Kind kind, @Nullable String marker, @NonNull List<Run> runs, boolean lead) {
            this.kind = kind;
            this.marker = marker;
            this.runs = runs;
            this.lead = lead;
        }

        @NonNull
        public String plainText() {
            StringBuilder sb = new StringBuilder();
            for (Run r : runs) sb.append(r.text);
            return sb.toString();
        }
    }

    private static final Pattern HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s+(.*)$");
    private static final Pattern ORDERED = Pattern.compile("^\\s*(\\d{1,2})[.、)）](?!\\d)\\s*(.+)$");
    private static final Pattern BULLET = Pattern.compile("^\\s*[-*•·]\\s+(.+)$");
    private static final Pattern WHOLE_BOLD = Pattern.compile("^\\s*\\*\\*([^*]+)\\*\\*\\s*$");
    private static final Pattern RULE = Pattern.compile("^\\s*([-*_])(\\s*\\1){2,}\\s*$");

    private AiChatMarkdown() {
    }

    @NonNull
    public static List<Block> parse(@Nullable String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        List<Block> out = new ArrayList<>();
        for (String raw : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            if (raw.trim().isEmpty() || RULE.matcher(raw).matches()) continue;
            Matcher m;
            if ((m = HEADING.matcher(raw)).matches()) {
                out.add(new Block(Kind.HEADING, null, plainRuns(m.group(1)), false));
            } else if ((m = WHOLE_BOLD.matcher(raw)).matches()) {
                out.add(new Block(Kind.HEADING, null, plainRuns(m.group(1)), false));
            } else if ((m = ORDERED.matcher(raw)).matches()) {
                out.add(new Block(Kind.ORDERED, m.group(1) + ".", runs(m.group(2)), false));
            } else if ((m = BULLET.matcher(raw)).matches()) {
                out.add(new Block(Kind.BULLET, "•", runs(m.group(1)), false));
            } else {
                out.add(new Block(Kind.PARAGRAPH, null, runs(raw.trim()), out.isEmpty()));
            }
        }
        return out;
    }

    /** 复制用的纯文字：去掉 Markdown 标记，列表保留序号 */
    @NonNull
    public static String toPlainText(@Nullable String text) {
        StringBuilder sb = new StringBuilder();
        for (Block b : parse(text)) {
            if (sb.length() > 0) sb.append('\n');
            if (b.marker != null) sb.append(b.marker).append(' ');
            sb.append(b.plainText());
        }
        return sb.toString();
    }

    /** 标题内不再区分粗体，去掉残留的星号 */
    private static List<Run> plainRuns(String s) {
        String t = s.replace("**", "").trim();
        return Collections.singletonList(new Run(t, false));
    }

    /** 拆分「**粗体**」；未闭合的「**」之后按粗体处理 */
    @NonNull
    static List<Run> runs(@NonNull String s) {
        List<Run> out = new ArrayList<>();
        boolean bold = false;
        int i = 0;
        while (i <= s.length()) {
            int next = s.indexOf("**", i);
            String part = next < 0 ? s.substring(i) : s.substring(i, next);
            if (!part.isEmpty()) out.add(new Run(part, bold));
            if (next < 0) break;
            bold = !bold;
            i = next + 2;
        }
        return out;
    }
}
