package com.heban.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.TaskItem;
import com.heban.app.data.model.WeatherState;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.TaskRepository;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.data.repository.WeatherRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final List<String> calls = new ArrayList<>();
    private final BehaviorProcessor<WeatherState> weather = BehaviorProcessor.create();
    private final BehaviorProcessor<List<TaskItem>> tasks = BehaviorProcessor.create();
    private final BehaviorProcessor<List<CropCard>> crops = BehaviorProcessor.create();
    private final BehaviorProcessor<Long> userIds = BehaviorProcessor.createDefault(0L);
    private final BehaviorProcessor<UserEntity> user = BehaviorProcessor.create();
    private Completable refreshResult = Completable.complete();
    private Completable setDoneResult = Completable.complete();
    private long now = at(15, 2);

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private static long at(int hour, int minute) {
        return LocalDateTime.of(2026, 10, 10, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private HomeViewModel create() {
        WeatherRepository w = HomeFakes.fake(WeatherRepository.class, calls, (name, args) -> {
            if (name.equals("observe")) return weather;
            if (name.equals("refresh")) return refreshResult;
            return null;
        });
        TaskRepository t = HomeFakes.fake(TaskRepository.class, calls, (name, args) -> {
            if (name.equals("observeToday")) return tasks;
            if (name.equals("setDone")) return setDoneResult;
            return null;
        });
        CropRepository c = HomeFakes.fake(CropRepository.class, calls, (name, args) ->
                name.equals("observeCropCards") ? crops : null);
        UserRepository u = HomeFakes.fake(UserRepository.class, calls, (name, args) ->
                name.equals("currentUser") ? user : null);
        return new HomeViewModel(null, w, t, c, u, userIds, () -> now);
    }

    // ---- 问候语 ----

    @Test
    public void greeting_byTimeOfDay() {
        assertEquals(R.string.home_greeting_evening, HomeViewModel.greetingRes(4));
        assertEquals(R.string.home_greeting_morning, HomeViewModel.greetingRes(5));
        assertEquals(R.string.home_greeting_morning, HomeViewModel.greetingRes(10));
        assertEquals(R.string.home_greeting_noon, HomeViewModel.greetingRes(11));
        assertEquals(R.string.home_greeting_noon, HomeViewModel.greetingRes(12));
        assertEquals(R.string.home_greeting_afternoon, HomeViewModel.greetingRes(13));
        assertEquals(R.string.home_greeting_afternoon, HomeViewModel.greetingRes(17));
        assertEquals(R.string.home_greeting_evening, HomeViewModel.greetingRes(18));
        assertEquals(R.string.home_greeting_evening, HomeViewModel.greetingRes(23));
    }

    @Test
    public void greeting_updatesWithClock() {
        HomeViewModel vm = create();
        assertEquals(R.string.home_greeting_afternoon, (int) vm.greetingRes.getValue());
        now = at(20, 0);
        vm.updateClock();
        assertEquals(R.string.home_greeting_evening, (int) vm.greetingRes.getValue());
        assertEquals(now, (long) vm.now.getValue());
    }

    // ---- 昵称与头像 ----

    @Test
    public void guest_hasNoNickname() {
        HomeViewModel vm = create();
        assertNull(vm.nickname.getValue());
        assertNull(vm.avatarText.getValue());
    }

    @Test
    public void signedIn_showsNicknameAndFirstChar() {
        HomeViewModel vm = create();
        UserEntity e = new UserEntity();
        e.nickname = "老李";
        user.onNext(e);
        userIds.onNext(1L);
        assertEquals("老李", vm.nickname.getValue());
        assertEquals("老", vm.avatarText.getValue());

        userIds.onNext(0L);
        assertNull(vm.nickname.getValue());
    }

    @Test
    public void firstChar_handlesSurrogatesAndEmpty() {
        assertNull(HomeViewModel.firstChar(""));
        assertNull(HomeViewModel.firstChar(null));
        assertEquals("🌾", HomeViewModel.firstChar("🌾农友"));
    }

    // ---- 合并状态 ----

    @Test
    public void state_combinesAllStreams() {
        HomeViewModel vm = create();
        HomeState initial = vm.state.getValue();
        assertTrue(initial.weather.showSkeleton());
        assertFalse(initial.loaded);
        assertFalse(initial.isTasksEmpty());              // 加载前不显示空状态
        assertFalse(initial.isCropsEmpty());

        weather.onNext(HomeFakes.loading());
        tasks.onNext(HomeFakes.tasks(HomeFakes.task(1, "a", true), HomeFakes.task(2, "b", false),
                HomeFakes.task(3, "c", false)));
        assertFalse(vm.state.getValue().loaded);          // 作物还没发射，保持初始状态

        crops.onNext(Collections.singletonList(HomeFakes.crop(1, "番茄")));
        HomeState s = vm.state.getValue();
        assertEquals(3, s.getTaskCount());
        assertEquals(1, s.getDoneCount());
        assertEquals(1, s.getCropCount());
        assertTrue(s.loaded);
        assertTrue(s.hasTasks());
        assertTrue(s.hasCrops());
        assertFalse(s.isTasksEmpty());
        assertFalse(s.isShowUnreadDot());
        assertTrue(s.weather.showSkeleton());
    }

    @Test
    public void weatherStreamError_becomesErrorState() {
        HomeViewModel vm = create();
        weather.onError(new BusinessException("NO_KEY", "未配置和风天气 Key"));
        tasks.onNext(Collections.emptyList());
        crops.onNext(Collections.emptyList());
        HomeState s = vm.state.getValue();
        assertTrue(s.weather.showError());
        assertEquals("未配置和风天气 Key", s.weather.errorMessage());
        assertFalse(s.weather.canRetry());
        assertTrue(s.isTasksEmpty());
    }

    // ---- 下拉刷新 / 勾选 ----

    @Test
    public void refresh_forcesWeatherRefresh() {
        HomeViewModel vm = create();
        vm.refresh();
        assertTrue(calls.contains("refresh:true"));
        assertFalse(vm.refreshing.getValue());
    }

    @Test
    public void refresh_errorShowsToastAndStops() {
        refreshResult = Completable.error(new IOException("offline"));
        HomeViewModel vm = create();
        vm.refresh();
        assertFalse(vm.refreshing.getValue());
        assertEquals("网络不太好，检查后再试一次", vm.toast.getValue());
    }

    @Test
    public void setDone_writesToRepository() {
        HomeViewModel vm = create();
        vm.setDone(HomeFakes.task(7, "灌溉", false), true);
        assertTrue(calls.contains("setDone:7:true"));
    }

    @Test
    public void setDone_errorShowsToast() {
        setDoneResult = Completable.error(new BusinessException("NOT_LOGGED_IN", "请先登录"));
        HomeViewModel vm = create();
        vm.setDone(HomeFakes.task(7, "灌溉", false), true);
        assertEquals("请先登录", vm.toast.getValue());
    }

    @Test
    public void userStreamUsesCurrentUserOnlyWhenSignedIn() {
        create();
        assertFalse(calls.contains("currentUser"));
        userIds.onNext(3L);
        assertTrue(calls.contains("currentUser"));
    }

    @Test
    public void unreadDefaultsToZero() {
        HomeViewModel vm = create();
        weather.onNext(HomeFakes.loading());
        tasks.onNext(Collections.emptyList());
        crops.onNext(Collections.emptyList());
        assertEquals(0, vm.state.getValue().unread);
        Flowable<Integer> unread = vm.unreadMessages();
        assertEquals(0, (int) unread.blockingFirst());
    }
}
