package com.heban.app.data.remote.llm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import com.heban.app.data.model.ScanResult;

import org.junit.Test;

public class ScanResultParserTest {
    private static final String JSON = "{\"image_ok\": true, \"image_issue\": \"\", \"crop\": \"黄瓜\",\n"
            + " \"top\": {\"name\": \"黄瓜霜霉病\", \"category\": \"卵菌病害\", \"part\": \"叶部\",\n"
            + "         \"confidence\": 0.92, \"severity\": \"中度\",\n"
            + "         \"evidence\": \"叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层\",\n"
            + "         \"advice\": [\"上午通风排湿\", \"摘除病叶带出棚外\", \"发病初期选用对症药剂，按标签用量喷施\"],\n"
            + "         \"urgency_days\": 3},\n"
            + " \"others\": [{\"name\": \"黄瓜细菌性角斑病\", \"confidence\": 0.06},\n"
            + "            {\"name\": \"缺镁症\", \"confidence\": 0.02}]}";

    private final Gson gson = new Gson();

    @Test
    public void parsesNormalJson() {
        ScanResult r = ScanResultParser.parse(JSON, gson);
        assertTrue(r.imageOk);
        assertEquals("黄瓜", r.crop);
        assertEquals("黄瓜霜霉病", r.top.name);
        assertEquals("卵菌病害", r.top.category);
        assertEquals("叶部", r.top.part);
        assertEquals(0.92, r.top.confidence, 1e-9);
        assertEquals("中度", r.top.severity);
        assertEquals(3, r.top.advice.size());
        assertEquals(3, r.top.urgencyDays);
        assertEquals(2, r.others.size());
        assertEquals("缺镁症", r.others.get(1).name);
    }

    @Test
    public void stripsCodeFences() {
        ScanResult r = ScanResultParser.parse("```json\n" + JSON + "\n```", gson);
        assertEquals("黄瓜霜霉病", r.top.name);
    }

    @Test
    public void ignoresTextAroundJson() {
        ScanResult r = ScanResultParser.parse("好的，识别结果如下：\n" + JSON + "\n以上仅供参考。", gson);
        assertEquals(0.92, r.top.confidence, 1e-9);
    }

    @Test
    public void imageNotOk_withoutTop_isValid() {
        ScanResult r = ScanResultParser.parse(
                "{\"image_ok\": false, \"image_issue\": \"照片过暗\", \"crop\": \"\", \"top\": null, \"others\": []}", gson);
        assertFalse(r.imageOk);
        assertEquals("照片过暗", r.imageIssue);
        assertNull(r.top);
        assertTrue(r.others.isEmpty());
    }

    @Test
    public void healthyPlant() {
        ScanResult r = ScanResultParser.parse("{\"crop\":\"番茄\",\"top\":{\"name\":\"健康\",\"category\":\"健康\","
                + "\"confidence\":0.88}}", gson);
        assertTrue(r.imageOk);   // 缺省为 true
        assertEquals("健康", r.top.category);
        assertTrue(r.top.advice.isEmpty());
        assertTrue(r.others.isEmpty());
    }

    @Test
    public void confidenceOutOfRange_fails() {
        expectFail(JSON.replace("0.92", "92"));
        expectFail(JSON.replace("0.92", "1.2"));
        expectFail(JSON.replace("0.92", "-0.1"));
        expectFail(JSON.replace("0.06", "6"));
    }

    @Test
    public void boundaryConfidence_ok() {
        assertEquals(1.0, ScanResultParser.parse(JSON.replace("0.92", "1"), gson).top.confidence, 1e-9);
        assertEquals(0.0, ScanResultParser.parse(JSON.replace("0.92", "0"), gson).top.confidence, 1e-9);
    }

    @Test
    public void missingFields_fail() {
        expectFail("{\"image_ok\": true, \"crop\": \"黄瓜\"}");
        expectFail("{\"image_ok\": true, \"top\": {\"name\": \"\", \"confidence\": 0.5}}");
    }

    @Test
    public void invalidJson_fails() {
        expectFail(null);
        expectFail("");
        expectFail("抱歉，我无法识别这张图片。");
        expectFail("} 反了 {");
        expectFail("{\"top\": {\"name\": \"x\", \"confidence\": \"高\"}}");
        expectFail("{\"top\": ");
    }

    @Test
    public void trimsAndDropsBlankEntries() {
        ScanResult r = ScanResultParser.parse("{\"top\":{\"name\":\" 蚜虫 \",\"confidence\":0.7,"
                + "\"advice\":[\" 黄板诱杀 \",\"\",null]},\"others\":[{\"name\":\"\",\"confidence\":0.1},null]}", gson);
        assertEquals("蚜虫", r.top.name);
        assertEquals(1, r.top.advice.size());
        assertEquals("黄板诱杀", r.top.advice.get(0));
        assertTrue(r.others.isEmpty());
    }

    private void expectFail(String text) {
        try {
            ScanResultParser.parse(text, gson);
            fail("应当解析失败：" + text);
        } catch (ScanResultParser.ScanParseException e) {
            assertTrue(VisionErrors.shouldFallback(e));
        }
    }
}
