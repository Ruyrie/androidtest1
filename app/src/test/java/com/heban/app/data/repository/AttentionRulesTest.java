package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.repository.AttentionRules.Action;

import org.junit.Test;

public class AttentionRulesTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long DAY = 24L * 3600_000L;
    private static final String SCAN = CropEntity.ATTENTION_SCAN;
    private static final String WEATHER = CropEntity.ATTENTION_WEATHER;

    @Test
    public void marksWhenNothingSet() {
        assertEquals(Action.MARK, AttentionRules.weather(null, null, 0, "霜霉病风险", NOW));
    }

    @Test
    public void quietForThreeDaysAfterDismiss() {
        assertEquals(Action.KEEP, AttentionRules.weather(null, null, NOW - DAY, "霜霉病风险", NOW));
        assertEquals(Action.KEEP, AttentionRules.weather(null, null, NOW - 3 * DAY + 1, "霜霉病风险", NOW));
        assertEquals(Action.MARK, AttentionRules.weather(null, null, NOW - 3 * DAY, "霜霉病风险", NOW));
        assertEquals(Action.MARK, AttentionRules.weather(null, null, NOW - 5 * DAY, "霜霉病风险", NOW));
    }

    @Test
    public void neverOverridesScanResult() {
        assertEquals(Action.KEEP, AttentionRules.weather("灰霉病", SCAN, 0, "霜霉病风险", NOW));
        assertEquals(Action.KEEP, AttentionRules.weather("灰霉病", SCAN, 0, null, NOW));
    }

    @Test
    public void updatesOrKeepsWeatherMark() {
        assertEquals(Action.KEEP, AttentionRules.weather("霜霉病风险", WEATHER, 0, "霜霉病风险", NOW));
        assertEquals(Action.MARK, AttentionRules.weather("霜霉病风险", WEATHER, 0, "冻害风险", NOW));
    }

    @Test
    public void clearsOnlyWeatherMarks() {
        assertEquals(Action.CLEAR, AttentionRules.weather("霜霉病风险", WEATHER, 0, null, NOW));
        assertEquals(Action.CLEAR, AttentionRules.weather("霜霉病风险", WEATHER, 0, "  ", NOW));
        assertEquals(Action.KEEP, AttentionRules.weather(null, null, 0, null, NOW));
    }

    @Test
    public void entityOverload() {
        CropEntity c = new CropEntity();
        c.attentionNote = "霜霉病风险";
        c.attentionSource = WEATHER;
        assertEquals(Action.CLEAR, AttentionRules.weather(c, null, NOW));
    }
}
