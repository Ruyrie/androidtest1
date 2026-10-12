package com.heban.app.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.repository.PestQueries;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class PestsJsonTest {
    private static final String REMINDER = "按产品标签用量使用，注意安全间隔期";
    private static final String[][] EXPECTED = {
            {"黄瓜霜霉病", "cucumber_downy_mildew"}, {"番茄灰霉病", "tomato_gray_mold"}, {"白粉病", "powdery_mildew"},
            {"蚜虫", "aphid"}, {"红蜘蛛（叶螨）", "spider_mite"}, {"番茄晚疫病", "tomato_late_blight"},
            {"黄瓜细菌性角斑病", "angular_leaf_spot"}, {"缺镁症", "magnesium_deficiency"},
            {"番茄早疫病", "tomato_early_blight"}, {"番茄叶霉病", "tomato_leaf_mold"},
            {"番茄黄化曲叶病毒病", "tomato_yellow_leaf_curl"}, {"烟粉虱", "whitefly"}, {"蓟马", "thrips"},
            {"小菜蛾", "diamondback_moth"}, {"菜青虫", "cabbage_white_caterpillar"},
            {"斜纹夜蛾", "spodoptera_litura"}, {"草地贪夜蛾", "fall_armyworm"}, {"稻瘟病", "rice_blast"},
            {"稻飞虱", "rice_planthopper"}, {"小麦条锈病", "wheat_stripe_rust"},
            {"小麦赤霉病", "wheat_fusarium_head_blight"}, {"玉米螟", "corn_borer"},
            {"草莓灰霉病", "strawberry_gray_mold"}, {"辣椒疫病", "pepper_phytophthora_blight"},
            {"根结线虫", "root_knot_nematode"}, {"猝倒病", "damping_off"}, {"炭疽病", "anthracnose"},
            {"枯萎病", "fusarium_wilt"}, {"潜叶蝇", "leaf_miner"}, {"缺氮症", "nitrogen_deficiency"},
            {"软腐病", "soft_rot"}};
    /** 化学防治只写通用名，不写具体用量 */
    private static final Pattern DOSE = Pattern.compile("\\d+\\s*(倍|克|g\\b|毫升|ml|mL|kg|公斤|斤|亩)");

    private final Gson gson = new Gson();

    @Test
    public void containsAllEntriesInOrderWithImagePaths() throws Exception {
        List<PestEntity> all = MockFixture.pests();
        assertEquals(EXPECTED.length, all.size());
        for (int i = 0; i < EXPECTED.length; i++) {
            PestEntity p = all.get(i);
            assertEquals(i + 1, p.id);
            assertEquals(EXPECTED[i][0], p.name);
            String[] images = gson.fromJson(p.images, String[].class);
            String[] captions = gson.fromJson(p.imageCaptions, String[].class);
            if (i == 0) {
                assertEquals(Arrays.asList("images/pests/cucumber_downy_mildew_1.jpg",
                        "images/pests/cucumber_downy_mildew_2.jpg"), Arrays.asList(images));
            } else {
                assertEquals(Arrays.asList("images/pests/" + EXPECTED[i][1] + ".jpg"), Arrays.asList(images));
            }
            assertEquals(p.name, images.length, captions.length);
        }
    }

    @Test
    public void everyEntryIsWellFormed() throws Exception {
        Set<String> kinds = new HashSet<>(Arrays.asList(PestEntity.KIND_DISEASE, PestEntity.KIND_INSECT,
                PestEntity.KIND_PHYSIOLOGICAL));
        Set<String> names = new HashSet<>();
        for (PestEntity p : MockFixture.pests()) {
            String n = p.name;
            assertTrue(n, names.add(n));
            assertTrue(n, kinds.contains(p.kind));
            for (String field : new String[]{p.aliases, p.enName, p.category, p.crops, p.cropTags, p.part,
                    p.seasonText, p.pathogen, p.conditions, p.symptoms, p.tip, p.icon}) {
                assertNotNull(n, field);
                assertFalse(n, field.trim().isEmpty());
            }
            assertTrue(n + " icon " + p.icon, MockFixture.resFile("drawable/ic_" + p.icon + ".xml").exists());
            assertTrue(n + " tip format", p.tip.contains(" · "));
            if (PestQueries.months(p.months).isEmpty()) {
                assertNull(n, p.seasonLevel);
            } else {
                assertTrue(n + " level " + p.seasonLevel,
                        "高发".equals(p.seasonLevel) || "较高".equals(p.seasonLevel));
                assertEquals(n + " months", PestQueries.splitList(p.months).size(), PestQueries.months(p.months).size());
            }
            String[] agri = gson.fromJson(p.preventionAgri, String[].class);
            String[] chem = gson.fromJson(p.preventionChem, String[].class);
            assertTrue(n, agri.length >= 2);
            assertTrue(n, chem.length >= 2);
            assertEquals(n, REMINDER, chem[chem.length - 1]);
            for (String c : chem) {
                assertFalse(n + " dose in: " + c, DOSE.matcher(c).find());
            }
        }
    }

    @Test
    public void cucumberDownyMildewMatchesPrototype() throws Exception {
        PestEntity p = MockFixture.pests().get(0);
        assertEquals("Cucumber downy mildew", p.enName);
        assertEquals("卵菌病害", p.category);
        assertEquals("叶部", p.part);
        assertEquals("春秋高发", p.seasonText);
        assertEquals("古巴假霜霉菌", p.pathogen);
        assertEquals("15–24°C，相对湿度 85% 以上，叶面有水膜", p.conditions);
        assertEquals("黄瓜、甜瓜等葫芦科作物", p.crops);
        assertEquals("叶片正面出现受叶脉限制的黄色多角形病斑；湿度大时，叶背对应位置长出灰黑色霉层，"
                + "严重时病斑连片、叶片枯焦。", p.symptoms);
        assertEquals(Arrays.asList("选用抗病品种，培育无病壮苗", "膜下滴灌，上午通风排湿，降低棚内湿度",
                "及时摘除病叶，带出棚外处理"), Arrays.asList(gson.fromJson(p.preventionAgri, String[].class)));
        assertEquals(Arrays.asList("叶片正面症状", "叶背霉层"),
                Arrays.asList(gson.fromJson(p.imageCaptions, String[].class)));
        assertEquals("黄瓜、白菜等 · 秋季低温高湿易发", p.tip);
        assertEquals("coronavirus", p.icon);
        assertEquals("高发", p.seasonLevel);
    }

    @Test
    public void prototypeIconsAndKinds() throws Exception {
        assertEquals("blur_on", MockFixture.pest("番茄灰霉病").icon);
        assertEquals("bug_report", MockFixture.pest("蚜虫").icon);
        assertEquals(PestEntity.KIND_PHYSIOLOGICAL, MockFixture.pest("缺镁症").kind);
        assertEquals(PestEntity.KIND_INSECT, MockFixture.pest("红蜘蛛（叶螨）").kind);
        assertEquals("细菌病害", MockFixture.pest("黄瓜细菌性角斑病").category);
    }
}
