package com.heban.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.remote.BusinessException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AddTaskViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private CalendarFakeTaskRepository tasks;
    private AddTaskFakeRepositories.Crops crops;
    private AddTaskFakeRepositories.Weather weather;
    private AddTaskViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        tasks = new CalendarFakeTaskRepository();
        crops = new AddTaskFakeRepositories.Crops();
        crops.cards.add(AddTaskFakeRepositories.card(5, "番茄", "1号大棚"));
        crops.cards.add(AddTaskFakeRepositories.card(6, "黄瓜", "2号大棚"));
        weather = new AddTaskFakeRepositories.Weather();
        Clock clock = Clock.fixed(TODAY.atTime(20, 0).atZone(ZONE).toInstant(), ZONE);
        vm = new AddTaskViewModel(null, tasks, crops, weather, clock);
        vm.tipVisible.observeForever(v -> { });
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void defaults_matchPrototype() {
        vm.start(null, null, 0, null);
        assertEquals(FarmType.SPRAY, vm.type.getValue());
        assertEquals(TODAY.plusDays(1), vm.date.getValue());
        assertEquals(LocalTime.of(8, 0), vm.time.getValue());
        assertEquals(Boolean.TRUE, vm.remind.getValue());
        assertEquals(0, (int) vm.repeatIndex.getValue());
        assertEquals(5, vm.crop.getValue().id);   // 第一种作物
        assertEquals(2, vm.cropOptions.getValue().size());
        assertEquals("用药 · 番茄", vm.title());
    }

    @Test
    public void arguments_prefillForm() {
        vm.start("IRRIGATE", TODAY.plusDays(3).toEpochDay(), 6, null);
        assertEquals(FarmType.IRRIGATE, vm.type.getValue());
        assertEquals(LocalDate.of(2026, 10, 13), vm.date.getValue());
        assertEquals(6, vm.crop.getValue().id);
        assertEquals("灌溉 · 黄瓜", vm.title());
    }

    @Test
    public void presetTitle_isKept() {
        vm.start(null, null, 0, "  防治番茄灰霉病 ");
        vm.selectType(FarmType.OTHER);
        assertEquals("防治番茄灰霉病", vm.title());
    }

    @Test
    public void noCrops_titleIsTypeOnly() {
        crops.cards.clear();
        vm.start(null, null, 0, null);
        assertNull(vm.crop.getValue());
        assertEquals("用药", vm.title());
        assertNull(vm.buildTask().cropId);
    }

    @Test
    public void selectCropNone_keepsNoneOnRefresh() {
        vm.start(null, null, 0, null);
        vm.selectCrop(null);
        assertNull(vm.crop.getValue());
        assertEquals("用药", vm.title());
    }

    @Test
    public void tip_refreshesOnTypeAndDate() {
        weather.tip = "11 日 7:00–10:00 风力 1 级、无降水，适合喷药。";
        vm.start(null, null, 0, null);
        assertTrue(vm.tipVisible.getValue());
        vm.selectType(FarmType.SPRAY);   // 未变化，不重新请求
        weather.tip = "";
        vm.selectType(FarmType.HARVEST);
        assertFalse(vm.tipVisible.getValue());
        vm.selectDate(LocalDate.of(2026, 10, 20));
        assertEquals(Arrays.asList("SPRAY@2026-10-11", "HARVEST@2026-10-11", "HARVEST@2026-10-20"),
                weather.requests);
    }

    @Test
    public void save_buildsTaskWithRepeatAndReminder() {
        vm.start(null, null, 0, null);
        vm.selectTime(14, 30);
        vm.selectRepeat(3);   // 每 7 天，共 3 次
        vm.note.setValue(" 嘧霉胺 1000 倍液，重点喷叶背 ");
        vm.save();

        FarmTaskEntity t = tasks.savedTask;
        assertEquals("SPRAY", t.type);
        assertEquals("用药 · 番茄", t.title);
        assertEquals(Long.valueOf(5), t.cropId);
        assertEquals(LocalDate.of(2026, 10, 11).atTime(14, 30).atZone(ZONE).toInstant().toEpochMilli(), t.dueAt);
        assertEquals(30, t.remindMinutes);
        assertEquals("嘧霉胺 1000 倍液，重点喷叶背", t.note);
        assertEquals(7, tasks.savedEvery);
        assertEquals(3, tasks.savedTimes);
        assertEquals(Long.valueOf(42), vm.saved.getValue());
        assertEquals(Boolean.FALSE, vm.saving.getValue());
    }

    @Test
    public void save_withoutRepeatOrReminder() {
        vm.start(null, null, 0, null);
        vm.setRemind(false);
        vm.save();
        assertEquals(-1, tasks.savedTask.remindMinutes);
        assertNull(tasks.savedTask.note);
        assertEquals(0, tasks.savedEvery);
        assertEquals(1, tasks.savedTimes);
    }

    @Test
    public void save_errorShowsMessage() {
        tasks.saveError = new BusinessException("NOT_LOGGED_IN", "请先登录");
        vm.start(null, null, 0, null);
        vm.save();
        assertEquals("请先登录", vm.toast.getValue());
        assertNull(vm.saved.getValue());
        assertEquals(Boolean.FALSE, vm.saving.getValue());
    }
}
