package com.heban.app.ui.discover.pest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.ui.discover.DiscoverFakes;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PestsFormatTest {
    private static final String REMINDER = "按产品标签用量使用、注意安全间隔期";
    private final Gson gson = new Gson();

    @Test
    public void jsonList_parsesStringArray() {
        assertEquals(Arrays.asList("叶片正面症状", "叶背霉层"),
                PestsFormat.jsonList(gson, "[\"叶片正面症状\",\"叶背霉层\"]"));
    }

    @Test
    public void jsonList_skipsBlankAndNonStrings() {
        assertEquals(Arrays.asList("a", "b"), PestsFormat.jsonList(gson, "[\" a \", \"\", null, \"b\", {\"x\":1}]"));
    }

    @Test
    public void jsonList_invalidOrEmpty_returnsEmpty() {
        assertTrue(PestsFormat.jsonList(gson, null).isEmpty());
        assertTrue(PestsFormat.jsonList(gson, "").isEmpty());
        assertTrue(PestsFormat.jsonList(gson, "   ").isEmpty());
        assertTrue(PestsFormat.jsonList(gson, "[\"unclosed").isEmpty());
        assertTrue(PestsFormat.jsonList(gson, "{\"a\":1}").isEmpty());
        assertTrue(PestsFormat.jsonList(gson, "\"plain\"").isEmpty());
        assertTrue(PestsFormat.jsonList(gson, "[]").isEmpty());
    }

    @Test
    public void cardSubtitle_cropsAndCategory() {
        PestEntity p = DiscoverFakes.pest(1, "黄瓜霜霉病", "病害", "卵菌病害", "黄瓜", null);
        assertEquals("黄瓜 · 卵菌病害", PestsFormat.cardSubtitle(p));
        p.cropTags = "瓜类、草莓";
        p.category = "真菌病害";
        assertEquals("瓜类、草莓 · 真菌病害", PestsFormat.cardSubtitle(p));
        p.category = null;
        assertEquals("瓜类、草莓", PestsFormat.cardSubtitle(p));
        p.cropTags = null;
        assertEquals("", PestsFormat.cardSubtitle(p));
    }

    @Test
    public void bannerDesc_addsDengForSeveralCrops() {
        PestEntity p = DiscoverFakes.pest(1, "霜霉病", "病害", "卵菌病害", "黄瓜、白菜", "高发");
        p.tip = "秋季低温高湿易发";
        assertEquals("黄瓜、白菜等 · 秋季低温高湿易发", PestsFormat.bannerDesc(p));
        p.cropTags = "多种蔬菜";
        assertEquals("多种蔬菜 · 秋季低温高湿易发", PestsFormat.bannerDesc(p));
        p.tip = null;
        assertEquals("多种蔬菜", PestsFormat.bannerDesc(p));
        p.cropTags = null;
        p.tip = "高温干旱易发";
        assertEquals("高温干旱易发", PestsFormat.bannerDesc(p));
    }

    @Test
    public void isHigh_onlyGaoFa() {
        assertTrue(PestsFormat.isHigh("高发"));
        assertTrue(PestsFormat.isHigh(" 高发 "));
        assertFalse(PestsFormat.isHigh("较高"));
        assertFalse(PestsFormat.isHigh(null));
    }

    @Test
    public void labelReminder_appendedOnlyWhenMissing() {
        List<String> chem = Arrays.asList("发病初期选用烯酰吗啉等药剂喷施");
        assertEquals(Arrays.asList("发病初期选用烯酰吗啉等药剂喷施", REMINDER),
                PestsFormat.withLabelReminder(chem, REMINDER));

        List<String> already = Arrays.asList("选用烯酰吗啉等药剂，按标签用量喷施");
        assertSame(already, PestsFormat.withLabelReminder(already, REMINDER));

        assertTrue("没有化学防治时不补提醒",
                PestsFormat.withLabelReminder(Collections.emptyList(), REMINDER).isEmpty());
    }

    @Test
    public void images_zipWithCaptions() {
        List<PestDetailImage> list = PestDetailImage.zip(
                Arrays.asList("images/pests/a_1.jpg", "images/pests/a_2.jpg"),
                Collections.singletonList("叶片正面症状"));
        assertEquals(2, list.size());
        assertEquals("images/pests/a_1.jpg", list.get(0).path);
        assertEquals("叶片正面症状", list.get(0).caption);
        assertNull(list.get(1).caption);

        List<PestDetailImage> captionsOnly = PestDetailImage.zip(Collections.emptyList(),
                Arrays.asList("叶片正面症状", "叶背霉层"));
        assertEquals(2, captionsOnly.size());
        assertNull(captionsOnly.get(1).path);
        assertEquals("叶背霉层", captionsOnly.get(1).caption);

        List<PestDetailImage> none = PestDetailImage.zip(Collections.emptyList(), Collections.emptyList());
        assertEquals("没有图片时显示一张占位", 1, none.size());
        assertNull(none.get(0).path);
        assertNull(none.get(0).caption);
    }

    @Test
    public void icons_fallbackByKind() {
        PestEntity p = DiscoverFakes.pest(1, "蚜虫", PestEntity.KIND_INSECT, "虫害", null, null);
        p.icon = "bug_report";
        assertEquals(com.heban.app.R.drawable.ic_bug_report, PestsIcons.forPest(p));
        p.icon = "unknown_symbol";
        assertEquals(com.heban.app.R.drawable.ic_bug_report, PestsIcons.forPest(p));
        p.kind = PestEntity.KIND_DISEASE;
        assertEquals(com.heban.app.R.drawable.ic_coronavirus, PestsIcons.forPest(p));
        p.icon = "blur_on";
        assertEquals(com.heban.app.R.drawable.ic_blur_on, PestsIcons.forPest(p));
    }
}
