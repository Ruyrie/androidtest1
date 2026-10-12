package com.heban.app.ui.discover.ai;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 {@link AiChatMarkdown} 的块排成一个 Spannable，与原型 AIChat.dc.html 的回答气泡一致：
 * 第一段 16/24 正文；其余段落 14/20 On Surface Variant；标题 16/24 Medium；列表 14/22，
 * 序号悬挂在 20dp 缩进内；块之间 6dp；行内粗体为 700 + 强调色（浅色 On Primary Container，深色 Primary）。
 */
public final class AiChatSpans {
    private AiChatSpans() {
    }

    @NonNull
    public static CharSequence render(@NonNull Context context, @Nullable String markdown) {
        List<AiChatMarkdown.Block> blocks = AiChatMarkdown.parse(markdown);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (blocks.isEmpty()) return sb;

        int onSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0);
        int onVariant = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOnSurfaceVariant, 0);
        int strong = strongColor(context);
        int sp16 = sp(context, 16);
        int sp14 = sp(context, 14);
        int gap = dp(context, 6);
        int indent = dp(context, 20);
        int markerGap = dp(context, 4);
        Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        for (int i = 0; i < blocks.size(); i++) {
            AiChatMarkdown.Block b = blocks.get(i);
            boolean last = i == blocks.size() - 1;
            AiChatMarkdown.Block next = last ? null : blocks.get(i + 1);
            int start = sb.length();
            List<int[]> boldRanges = new ArrayList<>();
            for (AiChatMarkdown.Run r : b.runs) {
                int rs = sb.length();
                sb.append(r.text);
                if (r.bold && b.kind != AiChatMarkdown.Kind.HEADING) boldRanges.add(new int[]{rs, sb.length()});
            }
            if (!last) sb.append('\n');
            int end = sb.length();

            boolean list = b.kind == AiChatMarkdown.Kind.ORDERED || b.kind == AiChatMarkdown.Kind.BULLET;
            boolean sameListNext = list && next != null && next.kind == b.kind;
            int size;
            int lineHeight;
            int color;
            if (b.kind == AiChatMarkdown.Kind.HEADING) {
                size = sp16;
                lineHeight = sp(context, 24);
                color = onSurface;
                sb.setSpan(new TypefaceSpan(medium), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (list) {
                size = sp14;
                lineHeight = sp(context, 22);
                color = onSurface;
                sb.setSpan(new MarkerSpan(b.marker, indent, markerGap, sp14, onSurface), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (b.lead) {
                size = sp16;
                lineHeight = sp(context, 24);
                color = onSurface;
            } else {
                size = sp14;
                lineHeight = sp(context, 20);
                color = onVariant;
            }
            sb.setSpan(new AbsoluteSizeSpan(size), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new BlockSpan(lineHeight, last || sameListNext ? 0 : gap), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // 粗体在块颜色之后设置，强调色覆盖块颜色
            for (int[] r : boldRanges) {
                sb.setSpan(new StyleSpan(Typeface.BOLD), r[0], r[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(strong), r[0], r[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sb;
    }

    /** 强调色：浅色 On Primary Container（#2E4F1C），深色 Primary（#AAD291），与原型一致 */
    @ColorInt
    static int strongColor(@NonNull Context context) {
        boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int attr = night ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnPrimaryContainer;
        return MaterialColors.getColor(context, attr, 0);
    }

    private static int sp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                c.getResources().getDisplayMetrics()));
    }

    private static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    /** 固定行高（同 LineHeightSpan.Standard），并在块的最后一行下方留出块间距 */
    static final class BlockSpan implements LineHeightSpan {
        private final int height;
        private final int gapAfter;

        BlockSpan(int height, int gapAfter) {
            this.height = height;
            this.gapAfter = gapAfter;
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight,
                                 Paint.FontMetricsInt fm) {
            int origin = fm.descent - fm.ascent;
            if (origin > 0) {
                float ratio = height * 1f / origin;
                fm.descent = Math.round(fm.descent * ratio);
                fm.ascent = fm.descent - height;
            }
            fm.top = fm.ascent;
            fm.bottom = fm.descent;
            if (gapAfter > 0 && text instanceof Spanned && end == ((Spanned) text).getSpanEnd(this)) {
                fm.descent += gapAfter;
                fm.bottom = fm.descent;
            }
        }
    }

    /** 列表序号：画在缩进区内、紧贴正文左侧（同浏览器 ol / ul 的 outside 标记） */
    static final class MarkerSpan implements LeadingMarginSpan {
        private final String marker;
        private final int indent;
        private final int gap;
        private final int textSize;
        private final int color;

        MarkerSpan(String marker, int indent, int gap, int textSize, int color) {
            this.marker = marker == null ? "" : marker;
            this.indent = indent;
            this.gap = gap;
            this.textSize = textSize;
            this.color = color;
        }

        @Override
        public int getLeadingMargin(boolean first) {
            return indent;
        }

        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom,
                                      CharSequence text, int start, int end, boolean first, Layout layout) {
            if (!first || !(text instanceof Spanned) || ((Spanned) text).getSpanStart(this) != start) return;
            TextPaint paint = new TextPaint(p);
            paint.setTextSize(textSize);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(p.getTypeface());
            float w = paint.measureText(marker);
            float right = x + dir * (indent - gap);
            float left = dir > 0 ? right - w : right;
            c.drawText(marker, left, baseline, paint);
        }
    }
}
