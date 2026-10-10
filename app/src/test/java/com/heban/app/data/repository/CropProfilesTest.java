package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.model.CropProfile;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CropProfilesTest {

    @Test
    public void jsonContainsCommonCrops() throws Exception {
        List<CropProfile> all = ProfileFixture.profiles();
        String[][] expected = {
                {"番茄", "tomato"}, {"樱桃番茄", "cherry_tomato"}, {"黄瓜", "cucumber"}, {"草莓", "strawberry"},
                {"冬小麦", "winter_wheat"}, {"辣椒", "pepper"}, {"茄子", "eggplant"}, {"大白菜", "chinese_cabbage"},
                {"生菜", "lettuce"}, {"菠菜", "spinach"}, {"芹菜", "celery"}, {"萝卜", "radish"}, {"大蒜", "garlic"},
                {"水稻", "rice"}, {"玉米", "corn"}, {"马铃薯", "potato"}, {"西瓜", "watermelon"}, {"甜瓜", "melon"},
                {"豇豆", "cowpea"}, {"葡萄", "grape"}};
        assertEquals(expected.length, all.size());
        Set<String> names = new HashSet<>();
        for (int i = 0; i < expected.length; i++) {
            CropProfile p = all.get(i);
            assertEquals(expected[i][0], p.name);
            assertEquals("images/crops/" + expected[i][1] + ".jpg", p.image);
            assertTrue(names.add(p.name));
        }
    }

    @Test
    public void everyProfileIsWellFormed() throws Exception {
        for (CropProfile p : ProfileFixture.profiles()) {
            assertTrue(p.name, p.expectedDays > 0);
            assertTrue(p.name + " icon " + p.icon,
                    ProfileFixture.resFile("drawable/ic_" + p.icon + ".xml").exists());
            assertTrue(p.name, p.stages.size() >= 3);
            int prev = Integer.MIN_VALUE;
            boolean hasZero = false;
            for (CropProfile.Stage s : p.stages) {
                assertTrue(p.name + " stage order", s.startDay > prev);
                assertTrue(p.name + " stage within season", s.startDay < p.expectedDays);
                prev = s.startDay;
                if (s.startDay == 0) hasZero = true;
            }
            assertTrue(p.name + " needs a stage starting on day 0", hasZero);
        }
    }

    @Test
    public void prototypeIcons() throws Exception {
        assertEquals("nutrition", ProfileFixture.profile("番茄").icon);
        assertEquals("nutrition", ProfileFixture.profile("樱桃番茄").icon);
        assertEquals("eco", ProfileFixture.profile("黄瓜").icon);
        assertEquals("grass", ProfileFixture.profile("冬小麦").icon);
        assertEquals("psychiatry", ProfileFixture.profile("草莓").icon);
    }

    @Test
    public void matching() throws Exception {
        List<CropProfile> all = ProfileFixture.profiles();
        assertEquals("番茄", CropProfiles.match(all, "番茄").name);
        assertEquals("番茄", CropProfiles.match(all, " 番茄 ").name);
        assertEquals("樱桃番茄", CropProfiles.match(all, "樱桃番茄").name);
        // 包含表中作物名：取最长
        assertEquals("樱桃番茄", CropProfiles.match(all, "樱桃番茄苗").name);
        assertEquals("番茄", CropProfiles.match(all, "大番茄").name);
        // 表中作物名包含输入
        assertEquals("冬小麦", CropProfiles.match(all, "小麦").name);
        // 品种优先
        assertEquals("樱桃番茄", CropProfiles.match(all, "番茄", "樱桃番茄").name);
        assertEquals("草莓", CropProfiles.match(all, "草莓", "红颜").name);
    }

    @Test
    public void unknownFallsBackToDefault() throws Exception {
        CropProfile p = CropProfiles.match(ProfileFixture.profiles(), "火龙果");
        assertEquals("火龙果", p.name);
        assertEquals(90, p.expectedDays);
        assertEquals("eco", p.icon);
        assertNull(p.image);
        assertEquals(3, p.stages.size());
        assertEquals(90, CropProfiles.match(ProfileFixture.profiles(), null).expectedDays);
        // 单字不做反向包含匹配
        assertEquals(90, CropProfiles.match(ProfileFixture.profiles(), "瓜").expectedDays);
    }
}
