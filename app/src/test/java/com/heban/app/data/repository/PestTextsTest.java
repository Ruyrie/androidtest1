package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;

import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.mock.MockFixture;

import org.junit.Test;

import java.util.List;

public class PestTextsTest {

    @Test
    public void listSubtitlesMatchPestsPrototype() throws Exception {
        List<PestEntity> all = MockFixture.pests();
        String[] expected = {"黄瓜 · 卵菌病害", "番茄 · 真菌病害", "瓜类、草莓 · 真菌病害", "多种蔬菜 · 虫害",
                "草莓、茄果 · 虫害", "番茄、马铃薯 · 卵菌病害"};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], PestTexts.subtitle(all.get(i)));
        }
    }

    @Test
    public void seasonalCardsMatchDiscoverPrototype() throws Exception {
        PestEntity downy = MockFixture.pest("黄瓜霜霉病");
        assertEquals("霜霉病", PestTexts.shortName(downy));
        assertEquals("黄瓜 · 白菜", PestTexts.seasonCrops(downy));
        PestEntity gray = MockFixture.pest("番茄灰霉病");
        assertEquals("灰霉病", PestTexts.shortName(gray));
        assertEquals("番茄 · 草莓", PestTexts.seasonCrops(gray));
        PestEntity aphid = MockFixture.pest("蚜虫");
        assertEquals("蚜虫", PestTexts.shortName(aphid));
        assertEquals("多种蔬菜", PestTexts.seasonCrops(aphid));
    }

    @Test
    public void shortNameKeepsNamesThatWouldBecomeTooShort() throws Exception {
        assertEquals("玉米螟", PestTexts.shortName(MockFixture.pest("玉米螟")));
        assertEquals("稻瘟病", PestTexts.shortName(MockFixture.pest("稻瘟病")));
        assertEquals("红蜘蛛（叶螨）", PestTexts.shortName(MockFixture.pest("红蜘蛛（叶螨）")));
        assertEquals("条锈病", PestTexts.shortName(MockFixture.pest("小麦条锈病")));
    }

    @Test
    public void seasonCropsFallsBackToTags() {
        PestEntity p = new PestEntity();
        p.name = "测试病";
        p.cropTags = "番茄、辣椒";
        p.tip = "没有分隔符";
        assertEquals("番茄 · 辣椒", PestTexts.seasonCrops(p));
        p.category = null;
        assertEquals("番茄、辣椒", PestTexts.subtitle(p));
    }
}
