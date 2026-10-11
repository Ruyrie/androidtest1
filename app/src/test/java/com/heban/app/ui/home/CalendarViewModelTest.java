package com.heban.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.TaskItem;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.StringReader;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CalendarViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);
    private static final String TERMS_JSON = "{\"tips\":{\"霜降\":\"注意防霜冻。\"},\"terms\":["
            + "{\"date\":\"2026-10-08\",\"name\":\"寒露\"},"
            + "{\"date\":\"2026-10-23\",\"name\":\"霜降\"},"
            + "{\"date\":\"2026-11-07\",\"name\":\"立冬\"}]}";

    private CalendarFakeTaskRepository repo;
    private CalendarViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new CalendarFakeTaskRepository();
        Map<LocalDate, Set<String>> oct = new HashMap<>();
        oct.put(LocalDate.of(2026, 10, 3), set(FarmType.FERTILIZE));
        oct.put(LocalDate.of(2026, 10, 5), set(FarmType.IRRIGATE));
        oct.put(LocalDate.of(2026, 10, 10), set(FarmType.FERTILIZE, FarmType.SPRAY, FarmType.IRRIGATE));
        oct.put(LocalDate.of(2026, 10, 11), set(FarmType.SPRAY));
        oct.put(LocalDate.of(2026, 10, 13), set(FarmType.HARVEST, FarmType.PRUNE));
        repo.typesByMonth.put(YearMonth.of(2026, 10), oct);
        repo.tasksByDay.put(TODAY, Arrays.asList(task(1, FarmType.FERTILIZE), task(2, FarmType.SPRAY)));
        Clock clock = Clock.fixed(TODAY.atTime(9, 30).atZone(ZONE).toInstant(), ZONE);
        vm = new CalendarViewModel(null, repo, Single.just(SolarTerms.parse(new StringReader(TERMS_JSON))), clock);
        vm.empty.observeForever(v -> { });
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private static Set<String> set(FarmType... types) {
        Set<String> s = new HashSet<>();
        for (FarmType t : types) s.add(t.name());
        return s;
    }

    private static TaskItem task(long id, FarmType type) {
        TaskItem t = new TaskItem();
        t.id = id;
        t.type = type;
        t.title = "任务" + id;
        return t;
    }

    private CalendarDay cell(int dayOfMonth) {
        for (CalendarDay d : vm.days.getValue()) {
            if (d.date != null && d.date.getDayOfMonth() == dayOfMonth) return d;
        }
        throw new AssertionError("no day " + dayOfMonth);
    }

    @Test
    public void start_defaultsToToday() {
        vm.start(null);
        assertEquals(TODAY, vm.selected.getValue());
        assertEquals(YearMonth.of(2026, 10), vm.month.getValue());
        List<CalendarDay> days = vm.days.getValue();
        assertEquals(35, days.size());
        assertTrue(days.get(0).isBlank());
        assertTrue(days.get(3).isBlank());
        assertEquals("1", days.get(4).dayText());
        CalendarDay today = cell(10);
        assertTrue(today.today);
        assertTrue(today.selected);
        assertFalse(cell(9).today);
        assertEquals(2, vm.dayTasks.getValue().size());
        assertFalse(vm.empty.getValue());
    }

    @Test
    public void start_withArgumentOpensThatDay_onlyOnce() {
        vm.start(LocalDate.of(2026, 11, 7));
        assertEquals(YearMonth.of(2026, 11), vm.month.getValue());
        assertTrue(vm.days.getValue().stream().anyMatch(d -> d.selected && d.date.getDayOfMonth() == 7));
        vm.start(null);   // 视图重建时不改变选择
        assertEquals(LocalDate.of(2026, 11, 7), vm.selected.getValue());
    }

    @Test
    public void days_showTermsAndColourDots() {
        vm.start(null);
        assertEquals("寒露", cell(8).term);
        assertEquals("霜降", cell(23).term);
        assertNull(cell(9).term);

        assertTrue(cell(3).dotManage);
        assertFalse(cell(3).dotIrrigate);
        assertTrue(cell(5).dotIrrigate);
        assertTrue(cell(11).dotPest);
        assertFalse(cell(11).dotManage);
        CalendarDay ten = cell(10);
        assertTrue(ten.dotManage && ten.dotIrrigate && ten.dotPest);
        // 整枝、采收都归入「施肥 / 管理」
        assertTrue(cell(13).dotManage);
        assertFalse(cell(13).dotPest);
        assertFalse(cell(14).hasDots());
    }

    @Test
    public void selectDay_loadsTasksAndEmptyState() {
        vm.start(null);
        vm.onDayClicked(cell(12));
        assertEquals(LocalDate.of(2026, 10, 12), vm.selected.getValue());
        assertTrue(vm.empty.getValue());
        assertTrue(cell(12).selected);
        assertFalse(cell(10).selected);
        assertTrue(cell(10).today);
        // 月份未变，不重复订阅
        assertEquals(1, repo.monthRequests.size());
    }

    @Test
    public void monthNavigation_selectsFirstDayOrToday() {
        vm.start(null);
        vm.nextMonth();
        assertEquals(YearMonth.of(2026, 11), vm.month.getValue());
        assertEquals(LocalDate.of(2026, 11, 1), vm.selected.getValue());
        assertEquals(30 + 0, vm.days.getValue().size());   // 2026-11-01 是周日，没有空格
        assertFalse(vm.days.getValue().stream().anyMatch(d -> d.hasDots()));

        vm.previousMonth();
        assertEquals(TODAY, vm.selected.getValue());

        vm.previousMonth();
        assertEquals(LocalDate.of(2026, 9, 1), vm.selected.getValue());

        vm.goToday();
        assertEquals(TODAY, vm.selected.getValue());
        assertEquals(Arrays.asList(YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 10),
                YearMonth.of(2026, 9), YearMonth.of(2026, 10)), repo.monthRequests);
    }

    @Test
    public void upcomingTerm_followsSelectedDay() {
        vm.start(null);
        assertEquals("霜降", vm.upcomingTerm.getValue().name);
        assertEquals("注意防霜冻。", vm.upcomingTerm.getValue().tip);
        vm.select(LocalDate.of(2026, 10, 8));
        assertEquals("寒露", vm.upcomingTerm.getValue().name);
        vm.select(LocalDate.of(2026, 12, 1));
        assertNull(vm.upcomingTerm.getValue());
    }

    @Test
    public void addTask_emitsSelectedDay() {
        vm.start(null);
        vm.select(LocalDate.of(2026, 10, 15));
        vm.addTask();
        assertEquals(LocalDate.of(2026, 10, 15), vm.openAddTask.getValue());
    }

    @Test
    public void toggleAndDelete_callRepository() {
        vm.start(null);
        TaskItem t = task(7, FarmType.IRRIGATE);
        vm.toggleDone(t);
        assertEquals(Boolean.TRUE, repo.doneCalls.get(7L));
        t.done = true;
        vm.toggleDone(t);
        assertEquals(Boolean.FALSE, repo.doneCalls.get(7L));
        vm.delete(t);
        assertEquals(Collections.singletonList(7L), repo.deleted);
        assertNotNull(vm.toastRes.getValue());
        assertEquals(R.string.calendar_deleted, (int) vm.toastRes.getValue());
    }
}
