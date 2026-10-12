package com.heban.app.ui.discover.scan;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.R;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.ScanResult;
import com.heban.app.ui.crop.CropsFakeRepository;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScanFormatTest {
    private static final int[] TERTIARY = {com.google.android.material.R.attr.colorTertiaryContainer,
            com.google.android.material.R.attr.colorOnTertiaryContainer};
    private static final int[] WARNING = {R.attr.colorWarningContainer, R.attr.colorOnWarningContainer};
    private static final int[] ERROR = {com.google.android.material.R.attr.colorErrorContainer,
            com.google.android.material.R.attr.colorOnErrorContainer};
    private static final int[] HEALTHY = {com.google.android.material.R.attr.colorPrimaryContainer,
            com.google.android.material.R.attr.colorOnPrimaryContainer};

    private static ScanResult.Top top(String severity, String category, int urgency) {
        ScanResult.Top t = ScanFakes.sampleResult().top;
        t.severity = severity;
        t.category = category;
        t.urgencyDays = urgency;
        return t;
    }

    @Test
    public void percent_roundsAndClamps() {
        assertEquals(92, ScanFormat.percent(0.92));
        assertEquals(6, ScanFormat.percent(0.0649));
        assertEquals(100, ScanFormat.percent(1.4));
        assertEquals(0, ScanFormat.percent(-0.2));
        assertEquals(0, ScanFormat.percent(Double.NaN));
        assertEquals(12, ScanFormat.otherBarProgress(0.06));
        assertEquals(100, ScanFormat.otherBarProgress(0.8));
    }

    @Test
    public void severityColours() {
        assertEquals(ScanFormat.Severity.MILD, ScanFormat.severity(top("轻度", "真菌病害", 7)));
        assertEquals(ScanFormat.Severity.MODERATE, ScanFormat.severity(top("中度", "卵菌病害", 3)));
        assertEquals(ScanFormat.Severity.SEVERE, ScanFormat.severity(top("重度", "细菌病害", 1)));
        assertEquals(ScanFormat.Severity.HEALTHY, ScanFormat.severity(top("", "健康", 0)));
        assertEquals(ScanFormat.Severity.NONE, ScanFormat.severity(top(null, "杂草", 0)));
        assertEquals(ScanFormat.Severity.NONE, ScanFormat.severity(null));

        assertArrayEquals(TERTIARY, ScanFormat.severityColors(ScanFormat.Severity.MILD));
        assertArrayEquals(WARNING, ScanFormat.severityColors(ScanFormat.Severity.MODERATE));
        assertArrayEquals(ERROR, ScanFormat.severityColors(ScanFormat.Severity.SEVERE));
        assertArrayEquals(HEALTHY, ScanFormat.severityColors(ScanFormat.Severity.HEALTHY));
        assertNull(ScanFormat.severityColors(ScanFormat.Severity.NONE));

        assertEquals("中度", ScanFormat.severityLabel(top("中度", "卵菌病害", 3)));
        assertEquals("健康", ScanFormat.severityLabel(top("轻度", "健康", 0)));
        assertEquals("", ScanFormat.severityLabel(top(" ", "杂草", 0)));
    }

    @Test
    public void tags_addFastSpreadWithinThreeDays() {
        assertEquals(Arrays.asList("卵菌病害", "叶部", "传播快"), ScanFormat.tags(top("中度", "卵菌病害", 3), "传播快"));
        assertEquals(Arrays.asList("真菌病害", "叶部"), ScanFormat.tags(top("轻度", "真菌病害", 7), "传播快"));
        assertEquals(Arrays.asList("真菌病害", "叶部"), ScanFormat.tags(top("轻度", "真菌病害", 0), "传播快"));
        assertEquals(Arrays.asList("健康", "叶部"), ScanFormat.tags(top("", "健康", 1), "传播快"));
        assertTrue(ScanFormat.tags(null, "传播快").isEmpty());
    }

    @Test
    public void evidenceText_matchesPrototype() {
        ScanResult.Top t = top("中度", "卵菌病害", 3);
        t.evidence = "叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层，与霜霉病典型症状一致。";
        assertEquals("叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层，与霜霉病典型症状一致。建议 3 天内处理，防止扩散到整棚。",
                ScanFormat.evidenceText(t));

        ScanResult.Top slow = top("轻度", "真菌病害", 7);
        slow.evidence = "叶片有少量褐色小斑";
        assertEquals("叶片有少量褐色小斑。建议 7 天内处理。", ScanFormat.evidenceText(slow));

        ScanResult.Top healthy = top("", "健康", 3);
        healthy.evidence = "叶色浓绿，未见病斑";
        assertEquals("叶色浓绿，未见病斑。", ScanFormat.evidenceText(healthy));
    }

    @Test
    public void aiPrompt_followsTechDocTemplate() {
        assertEquals("拍照识别结果：黄瓜霜霉病（中度，匹配度 92%），依据：叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层。"
                + "请给出详细防治方案。", ScanFormat.aiPrompt(ScanFakes.sampleResult()));

        ScanResult noSeverity = ScanFakes.sampleResult();
        noSeverity.top.severity = null;
        noSeverity.top.evidence = "";
        assertEquals("拍照识别结果：黄瓜霜霉病（匹配度 92%）。请给出详细防治方案。", ScanFormat.aiPrompt(noSeverity));

        ScanResult healthy = ScanFakes.sampleResult();
        healthy.top.name = "健康的黄瓜叶片";
        healthy.top.category = "健康";
        healthy.top.evidence = "叶色浓绿，未见病斑。";
        assertEquals("拍照识别结果：健康的黄瓜叶片（健康，匹配度 92%），依据：叶色浓绿，未见病斑。请给出日常管理建议。",
                ScanFormat.aiPrompt(healthy));

        assertEquals("", ScanFormat.aiPrompt(null));
    }

    @Test
    public void publishText_isShortQuestion() {
        assertEquals("这是黄瓜霜霉病吗？拍照识别匹配度 92%，症状：叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层。请农友帮忙看看。",
                ScanFormat.publishText(ScanFakes.sampleResult()));
        ScanResult healthy = ScanFakes.sampleResult();
        healthy.top.category = "健康";
        healthy.top.evidence = null;
        assertEquals("这株黄瓜健康吗？拍照识别匹配度 92%。请农友帮忙看看。", ScanFormat.publishText(healthy));
    }

    @Test
    public void cropMatchingAndLabels() {
        CropCard tomato = CropsFakeRepository.card(1, "番茄", "", "1号大棚", 1, "", "", 1, 100, null);
        CropCard cucumber = CropsFakeRepository.card(2, "黄瓜", "", "2号大棚", 1, "", "", 1, 100, null);
        CropCard fruitCucumber = CropsFakeRepository.card(3, "水果黄瓜", "", null, 1, "", "", 1, 100, null);
        List<CropCard> all = Arrays.asList(fruitCucumber, tomato, cucumber);

        List<CropCard> matched = ScanFormat.matchingCrops(all, "黄瓜");
        assertEquals(2, matched.size());
        assertEquals(2, matched.get(0).id);      // 名称完全相同的在前
        assertEquals(3, matched.get(1).id);
        assertTrue(ScanFormat.matchingCrops(all, "辣椒").isEmpty());
        assertTrue(ScanFormat.matchingCrops(all, null).isEmpty());

        assertEquals("黄瓜 · 2号大棚", ScanFormat.cropLabel(cucumber));
        assertEquals("水果黄瓜", ScanFormat.cropLabel(fruitCucumber));

        assertEquals("水果黄瓜、番茄、黄瓜", ScanFormat.cropHint(all));
        assertNull(ScanFormat.cropHint(Collections.emptyList()));
    }

    @Test
    public void stripEnd_removesTrailingPunctuation() {
        assertEquals("有霉层", ScanFormat.stripEnd("有霉层。"));
        assertEquals("有霉层", ScanFormat.stripEnd("有霉层，。 "));
        assertEquals("", ScanFormat.stripEnd("。"));
    }
}
