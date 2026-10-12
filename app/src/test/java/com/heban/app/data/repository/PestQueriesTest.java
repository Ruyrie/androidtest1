package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.mock.MockFixture;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PestQueriesTest {
    private List<PestEntity> all;

    @Before
    public void setUp() throws Exception {
        all = MockFixture.pests();
    }

    private static List<String> names(List<PestEntity> list) {
        List<String> out = new ArrayList<>();
        for (PestEntity p : list) out.add(p.name);
        return out;
    }

    private String match(String q) {
        PestEntity p = PestQueries.match(all, q);
        return p == null ? null : p.name;
    }

    @Test
    public void monthsParsing() {
        assertEquals(Arrays.asList(3, 9, 10), new ArrayList<>(PestQueries.months("10, 3,9,x,13,3")));
        assertTrue(PestQueries.months(null).isEmpty());
        assertTrue(PestQueries.months("").isEmpty());
        assertEquals(Arrays.asList("黄瓜", "白菜"), PestQueries.splitList(" 黄瓜、白菜 ,"));
    }

    @Test
    public void octoberSeasonalMatchesDiscoverPrototype() {
        List<PestEntity> oct = PestQueries.seasonal(all, 10);
        assertEquals(Arrays.asList("黄瓜霜霉病", "番茄灰霉病", "蚜虫"), names(oct).subList(0, 3));
        assertEquals("高发", oct.get(0).seasonLevel);
        assertEquals("较高", oct.get(1).seasonLevel);
        assertEquals("较高", oct.get(2).seasonLevel);
        for (PestEntity p : oct) assertTrue(p.name, PestQueries.inSeason(p, 10));
        assertFalse(names(oct).contains("缺氮症"));
    }

    @Test
    public void seasonalPutsHighFirstThenId() {
        for (int m = 1; m <= 12; m++) {
            List<PestEntity> list = PestQueries.seasonal(all, m);
            assertFalse("month " + m, list.isEmpty());
            for (int i = 1; i < list.size(); i++) {
                PestEntity a = list.get(i - 1);
                PestEntity b = list.get(i);
                int ra = PestQueries.levelRank(a.seasonLevel);
                int rb = PestQueries.levelRank(b.seasonLevel);
                assertTrue(ra < rb || (ra == rb && a.id < b.id));
            }
        }
    }

    @Test
    public void searchByKeywordIgnoresCase() {
        assertEquals(Arrays.asList("黄瓜霜霉病"), names(PestQueries.filter(all, "DOWNY", null, null)));
        assertEquals(Arrays.asList("黄瓜霜霉病"), names(PestQueries.filter(all, "跑马干", null, null)));
        assertTrue(names(PestQueries.filter(all, "灰霉", null, null))
                .containsAll(Arrays.asList("番茄灰霉病", "草莓灰霉病")));
        // 作物：危害作物文字也能搜到
        assertTrue(names(PestQueries.filter(all, "甜瓜", null, null)).contains("白粉病"));
        assertEquals(all.size(), PestQueries.filter(all, "  ", null, null).size());
    }

    @Test
    public void kindAndCropFilters() {
        List<PestEntity> physio = PestQueries.filter(all, null, PestEntity.KIND_PHYSIOLOGICAL, null);
        assertEquals(Arrays.asList("缺镁症", "缺氮症"), names(physio));
        assertEquals(all.size(), PestQueries.filter(all, null, "全部", "全部").size());
        List<PestEntity> insects = PestQueries.filter(all, null, PestEntity.KIND_INSECT, null);
        for (PestEntity p : insects) assertEquals(PestEntity.KIND_INSECT, p.kind);
        assertTrue(names(insects).contains("蚜虫"));

        List<PestEntity> cucumber = PestQueries.filter(all, null, null, "黄瓜");
        assertTrue(names(cucumber).containsAll(Arrays.asList("黄瓜霜霉病", "黄瓜细菌性角斑病", "白粉病", "蚜虫")));
        assertFalse(names(cucumber).contains("稻瘟病"));
        List<PestEntity> riceInsects = PestQueries.filter(all, null, PestEntity.KIND_INSECT, "水稻");
        // 草地贪夜蛾的危害作物中也写了水稻
        assertEquals(Arrays.asList("草地贪夜蛾", "稻飞虱"), names(riceInsects));
        assertEquals(Arrays.asList("稻瘟病"), names(PestQueries.filter(all, "瘟", PestEntity.KIND_DISEASE, "水稻")));
    }

    @Test
    public void matchByName() {
        assertEquals("黄瓜霜霉病", match("黄瓜霜霉病"));
        assertEquals("黄瓜霜霉病", match(" 霜霉病 "));                  // 别名
        assertEquals("黄瓜霜霉病", match("cucumber downy mildew"));     // 英文名
        assertEquals("缺镁症", match("缺镁症（生理性）"));               // 去掉括号说明
        assertEquals("番茄灰霉病", match("疑似番茄灰霉病菌侵染"));        // 名称出现在查询中
        assertEquals("黄瓜细菌性角斑病", match("细菌性角斑病"));
        assertEquals("红蜘蛛（叶螨）", match("红蜘蛛"));
        assertEquals("番茄晚疫病", match("晚疫病叶片"));                  // 取最长的匹配词
        assertEquals("番茄灰霉病", match("灰霉"));                        // 名称包含查询
        assertNull(match("霉"));                                          // 少于 2 个字
        assertNull(match("苹果腐烂病"));
        assertNull(match(""));
        assertNull(match(null));
    }

    @Test
    public void cropOptionsSkipGroupWords() {
        List<String> options = PestQueries.cropOptions(all);
        assertFalse(options.contains("多种蔬菜"));
        assertFalse(options.contains("瓜类"));
        assertFalse(options.contains("茄果"));
        assertTrue(options.containsAll(Arrays.asList("黄瓜", "番茄", "草莓", "白菜", "水稻", "小麦", "玉米")));
        assertEquals(options.size(), new java.util.HashSet<>(options).size());
        assertEquals("番茄", options.get(0));   // 出现次数最多
        // 每个选项都能筛出至少一条
        for (String c : options) assertFalse(c, PestQueries.filter(all, null, null, c).isEmpty());
    }
}
