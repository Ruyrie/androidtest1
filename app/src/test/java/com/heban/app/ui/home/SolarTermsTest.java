package com.heban.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** 校验 assets/mock/solar_terms.json 的内容与解析。 */
public class SolarTermsTest {
    private static final List<String> ORDER = Arrays.asList(
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至");

    private static SolarTerms terms;

    @BeforeClass
    public static void load() throws Exception {
        File f = new File("src/main/assets/" + SolarTerms.ASSET);
        if (!f.exists()) f = new File("app/src/main/assets/" + SolarTerms.ASSET);
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            terms = SolarTerms.parse(r);
        }
    }

    @Test
    public void asset_has24TermsPerYearInOrder() {
        assertEquals(72, terms.size());
        for (int year = 2025; year <= 2027; year++) {
            List<SolarTerms.Term> list = terms.inYear(year);
            assertEquals("year " + year, 24, list.size());
            for (int i = 0; i < 24; i++) {
                SolarTerms.Term t = list.get(i);
                assertEquals(year + " #" + i, ORDER.get(i), t.name);
                // 每月两个节气：第 i 个落在 (i / 2 + 1) 月
                assertEquals(t.name, i / 2 + 1, t.date.getMonthValue());
                assertFalse(t.name + " tip", t.tip.isEmpty());
            }
        }
    }

    @Test
    public void asset_knownDates() {
        assertEquals("寒露", terms.nameOn(LocalDate.of(2026, 10, 8)));
        assertEquals("霜降", terms.nameOn(LocalDate.of(2026, 10, 23)));
        assertEquals("立春", terms.nameOn(LocalDate.of(2025, 2, 3)));
        assertEquals("立春", terms.nameOn(LocalDate.of(2026, 2, 4)));
        assertEquals("清明", terms.nameOn(LocalDate.of(2026, 4, 5)));
        assertEquals("冬至", terms.nameOn(LocalDate.of(2025, 12, 21)));
        assertEquals("冬至", terms.nameOn(LocalDate.of(2026, 12, 22)));
        assertEquals("霜降", terms.nameOn(LocalDate.of(2027, 10, 23)));
        assertNull(terms.nameOn(LocalDate.of(2026, 10, 10)));
    }

    @Test
    public void nextOnOrAfter_findsUpcomingTerm() {
        SolarTerms.Term t = terms.nextOnOrAfter(LocalDate.of(2026, 10, 10));
        assertNotNull(t);
        assertEquals("霜降", t.name);
        assertEquals(LocalDate.of(2026, 10, 23), t.date);
        assertEquals("注意防霜冻，露天地块适时完成冬小麦播种。", t.tip);

        assertEquals("寒露", terms.nextOnOrAfter(LocalDate.of(2026, 10, 8)).name);
        assertEquals("小寒", terms.nextOnOrAfter(LocalDate.of(2026, 12, 23)).name);
        assertNull(terms.nextOnOrAfter(LocalDate.of(2027, 12, 23)));
    }

    @Test
    public void parse_skipsBadEntriesAndMissingTips() {
        SolarTerms s = SolarTerms.parse(new StringReader("{\"terms\":["
                + "{\"date\":\"2026-10-08\",\"name\":\"寒露\"},"
                + "{\"date\":\"bad\",\"name\":\"霜降\"},"
                + "{\"name\":\"立冬\"},"
                + "{\"date\":\"2026-11-22\",\"name\":\"\"}]}"));
        assertEquals(1, s.size());
        assertEquals("寒露", s.nameOn(LocalDate.of(2026, 10, 8)));
        assertEquals("", s.nextOnOrAfter(LocalDate.of(2026, 10, 1)).tip);
    }

    @Test
    public void parse_invalidJsonIsEmpty() {
        assertEquals(0, SolarTerms.parse(new StringReader("{not json")).size());
        assertEquals(0, SolarTerms.parse(new StringReader("")).size());
        assertEquals(0, SolarTerms.parse(new StringReader("{}")).size());
    }
}
