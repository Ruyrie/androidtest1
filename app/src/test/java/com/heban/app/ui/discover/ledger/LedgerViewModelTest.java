package com.heban.app.ui.discover.ledger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.LedgerMonthSummary;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LedgerViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-10T02:00:00Z"), LedgerFakes.ZONE);
    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private LedgerFakes.Ledger repo;
    private LedgerViewModel vm;
    private Integer lastMessage;
    private String lastToast;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new LedgerFakes.Ledger();
        LedgerMonthSummary s = new LedgerMonthSummary();
        s.incomeCent = 520000;
        s.expenseCent = 174000;
        repo.summaries.put(OCT, s);
        repo.mixes.put(OCT, Arrays.asList(new CategoryShare("化肥", 72000, 41), new CategoryShare("农药", 41000, 24),
                new CategoryShare("人工", 40000, 23), new CategoryShare("其他", 21000, 12)));
        repo.days.put(OCT, Collections.singletonList(LedgerFakes.prototypeDay(TODAY)));
        vm = new LedgerViewModel(null, repo, CLOCK);
        vm.messageRes.observeForever(v -> lastMessage = v);
        vm.toast.observeForever(v -> lastToast = v);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void start_showsCurrentMonthLikePrototype() {
        vm.start();
        assertEquals(OCT, vm.month.getValue());
        assertEquals("2026年10月", vm.monthTitle.getValue());
        assertFalse(vm.canNext.getValue());
        assertTrue(vm.isCurrentMonth.getValue());
        assertEquals("+¥3,460", vm.balanceText.getValue());
        assertEquals("¥5,200", vm.incomeText.getValue());
        assertEquals("¥1,740", vm.expenseText.getValue());
        assertFalse(vm.empty.getValue());
        assertFalse(vm.showMix.getValue());
    }

    @Test
    public void start_onlyOnce() {
        vm.start();
        vm.previousMonth();
        vm.start();
        assertEquals(OCT.minusMonths(1), vm.month.getValue());
    }

    @Test
    public void monthSwitching_cannotGoPastCurrentMonth() {
        vm.start();
        vm.nextMonth();
        assertEquals(OCT, vm.month.getValue());

        vm.previousMonth();
        assertEquals(YearMonth.of(2026, 9), vm.month.getValue());
        assertEquals("2026年9月", vm.monthTitle.getValue());
        assertTrue(vm.canNext.getValue());
        assertFalse(vm.isCurrentMonth.getValue());

        vm.previousMonth();
        vm.previousMonth();
        assertEquals(YearMonth.of(2026, 7), vm.month.getValue());
        vm.nextMonth();
        vm.nextMonth();
        vm.nextMonth();
        assertEquals(OCT, vm.month.getValue());
        assertFalse(vm.canNext.getValue());
        vm.nextMonth();
        assertEquals(OCT, vm.month.getValue());
        assertEquals(Arrays.asList(OCT, YearMonth.of(2026, 9), YearMonth.of(2026, 8), YearMonth.of(2026, 7),
                YearMonth.of(2026, 8), YearMonth.of(2026, 9), OCT), repo.requestedMonths);
    }

    @Test
    public void previousMonth_acrossYear() {
        vm.start();
        for (int i = 0; i < 10; i++) vm.previousMonth();
        assertEquals(YearMonth.of(2025, 12), vm.month.getValue());
        assertEquals("2025年12月", vm.monthTitle.getValue());
    }

    @Test
    public void emptyMonth() {
        vm.start();
        vm.previousMonth();
        assertTrue(vm.empty.getValue());
        assertTrue(vm.rows.getValue().isEmpty());
        assertEquals("¥0", vm.balanceText.getValue());
        assertEquals("¥0", vm.incomeText.getValue());
        assertEquals("¥0", vm.expenseText.getValue());
        assertTrue(vm.mix.getValue().isEmpty());
    }

    @Test
    public void negativeBalance() {
        LedgerMonthSummary s = new LedgerMonthSummary();
        s.incomeCent = 10000;
        s.expenseCent = 38000;
        repo.summaries.put(OCT, s);
        vm.start();
        assertEquals("−¥280", vm.balanceText.getValue());
    }

    @Test
    public void charts_mappedFromRepository() {
        vm.start();
        List<LedgerChartModel.Bar> bars = vm.bars.getValue();
        assertEquals(6, bars.size());
        assertEquals("5月", bars.get(0).label);
        assertEquals("+820", bars.get(0).valueText);
        assertEquals("10月", bars.get(5).label);
        assertEquals("+3,460", bars.get(5).valueText);
        List<LedgerChartModel.MixRow> mix = vm.mix.getValue();
        assertEquals(4, mix.size());
        assertEquals("¥720 · 41%", mix.get(0).valueText);
        assertEquals(1f, mix.get(0).fraction, 0.001f);
    }

    @Test
    public void charts_negativeValuesKeptSigned() {
        repo.balancesYuan = new long[]{820, 1960, 3120, -280, 1450, 3460};
        vm.start();
        LedgerChartModel.Bar aug = vm.bars.getValue().get(3);
        assertEquals("8月", aug.label);
        assertEquals(-28000, aug.cents);
        assertEquals("−280", aug.valueText);
    }

    @Test
    public void rows_groupedByDay() {
        vm.start();
        List<LedgerRow> rows = vm.rows.getValue();
        assertEquals(3, rows.size());
        LedgerRow day = rows.get(0);
        assertEquals(LedgerRow.TYPE_DAY, day.type);
        assertEquals("今天 · 10月10日", day.title);
        assertEquals("收 960 · 支 60", day.amount);

        LedgerRow sale = rows.get(1);
        assertEquals(LedgerRow.TYPE_ITEM, sale.type);
        assertEquals("樱桃番茄 120 斤", sale.title);
        assertEquals("销售 · 番茄 · 1号大棚", sale.sub);
        assertEquals("+960", sale.amount);
        assertTrue(sale.income);
        assertFalse(sale.fromRecord);
        assertEquals(R.drawable.ic_sell, sale.icon);
        assertTrue(sale.first);
        assertFalse(sale.last);

        LedgerRow fert = rows.get(2);
        assertEquals("硫酸钾 5 kg", fert.title);
        assertEquals("化肥 · 番茄 · 来自农事记录", fert.sub);
        assertEquals("−60", fert.amount);
        assertFalse(fert.income);
        assertTrue(fert.fromRecord);
        assertEquals(R.drawable.ic_compost, fert.icon);
        assertFalse(fert.first);
        assertTrue(fert.last);
    }

    @Test
    public void viewToggle() {
        vm.start();
        vm.showMix();
        assertTrue(vm.showMix.getValue());
        vm.showTrend();
        assertFalse(vm.showMix.getValue());
    }

    @Test
    public void deleteEntry() {
        vm.start();
        vm.deleteEntry(11);
        assertEquals(Collections.singletonList(11L), repo.deleted);
        assertEquals(Integer.valueOf(R.string.ledger_deleted), lastMessage);
    }

    @Test
    public void deleteEntry_error() {
        repo.failWrite = true;
        vm.start();
        vm.deleteEntry(11);
        assertEquals("请先登录", lastToast);
    }
}
