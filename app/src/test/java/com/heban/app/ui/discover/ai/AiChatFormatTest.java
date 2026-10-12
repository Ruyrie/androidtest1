package com.heban.app.ui.discover.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.local.entity.PestEntity;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/** AiChatMarkdown / AiChatPests / AiChatTimes */
public class AiChatFormatTest {

    @Test
    public void markdown_blocksAndBold() {
        List<AiChatMarkdown.Block> b = AiChatMarkdown.parse(
                "初步判断为 **黄瓜霜霉病**。\n病斑呈多角形。\n\n### 防治建议\n1. 上午通风\n2、摘除病叶\n- 注意安全间隔期\n**小结：**");
        assertEquals(7, b.size());
        assertEquals(AiChatMarkdown.Kind.PARAGRAPH, b.get(0).kind);
        assertTrue(b.get(0).lead);
        assertEquals(3, b.get(0).runs.size());
        assertTrue(b.get(0).runs.get(1).bold);
        assertEquals("黄瓜霜霉病", b.get(0).runs.get(1).text);
        assertFalse(b.get(1).lead);
        assertEquals(AiChatMarkdown.Kind.HEADING, b.get(2).kind);
        assertEquals("防治建议", b.get(2).plainText());
        assertEquals(AiChatMarkdown.Kind.ORDERED, b.get(3).kind);
        assertEquals("1.", b.get(3).marker);
        assertEquals("上午通风", b.get(3).plainText());
        assertEquals("2.", b.get(4).marker);
        assertEquals(AiChatMarkdown.Kind.BULLET, b.get(5).kind);
        assertEquals(AiChatMarkdown.Kind.HEADING, b.get(6).kind);
        assertEquals("小结：", b.get(6).plainText());
    }

    @Test
    public void markdown_notListWhenNumberIsPartOfText() {
        List<AiChatMarkdown.Block> b = AiChatMarkdown.parse("10月适合种菠菜\n1.5公斤尿素");
        assertEquals(AiChatMarkdown.Kind.PARAGRAPH, b.get(0).kind);
        assertEquals(AiChatMarkdown.Kind.PARAGRAPH, b.get(1).kind);
    }

    @Test
    public void markdown_unclosedBoldWhileStreaming() {
        List<AiChatMarkdown.Block> b = AiChatMarkdown.parse("更像 **缺镁");
        assertEquals(2, b.get(0).runs.size());
        assertTrue(b.get(0).runs.get(1).bold);
        assertEquals("缺镁", b.get(0).runs.get(1).text);
    }

    @Test
    public void markdown_plainTextForCopy() {
        assertEquals("初步判断为 黄瓜霜霉病。\n防治建议\n1. 通风\n• 摘叶",
                AiChatMarkdown.toPlainText("初步判断为 **黄瓜霜霉病**。\n## 防治建议\n1. 通风\n* 摘叶"));
        assertTrue(AiChatMarkdown.parse(null).isEmpty());
    }

    @Test
    public void pests_matchFirstAppearingName() {
        PestEntity a = AiChatFakes.pest(1, "黄瓜细菌性角斑病", "黄瓜");
        PestEntity b = AiChatFakes.pest(2, "黄瓜霜霉病", "黄瓜、甜瓜");
        List<PestEntity> list = Arrays.asList(a, b);
        assertEquals(2, AiChatPests.match("初步判断为黄瓜霜霉病，需与黄瓜细菌性角斑病区分", list).id);
        assertNull(AiChatPests.match("番茄缺镁", list));
        assertEquals("霜霉病", AiChatPests.shortName(b));
        assertEquals("细菌性角斑病", AiChatPests.shortName(a));
        assertEquals("蚜虫", AiChatPests.shortName(AiChatFakes.pest(3, "蚜虫", "番茄")));
    }

    @Test
    public void times() {
        ZoneId z = ZoneId.of("Asia/Shanghai");
        long now = LocalDateTime.of(2026, 10, 11, 15, 0).atZone(z).toInstant().toEpochMilli();
        String[] f = {"今天 %1$s", "昨天 %1$s", "%1$d月%2$d日", "%1$d年%2$d月%3$d日"};
        assertEquals("今天 09:05", AiChatTimes.format(at(z, 2026, 10, 11, 9, 5), now, z, f[0], f[1], f[2], f[3]));
        assertEquals("昨天 23:59", AiChatTimes.format(at(z, 2026, 10, 10, 23, 59), now, z, f[0], f[1], f[2], f[3]));
        assertEquals("10月8日", AiChatTimes.format(at(z, 2026, 10, 8, 8, 0), now, z, f[0], f[1], f[2], f[3]));
        assertEquals("2025年3月1日", AiChatTimes.format(at(z, 2025, 3, 1, 8, 0), now, z, f[0], f[1], f[2], f[3]));
    }

    private static long at(ZoneId z, int y, int m, int d, int h, int min) {
        return LocalDateTime.of(y, m, d, h, min).atZone(z).toInstant().toEpochMilli();
    }
}
