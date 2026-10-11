package com.heban.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.data.model.WeatherState;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.WeatherRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class WeatherViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final List<String> calls = new ArrayList<>();
    private final BehaviorProcessor<WeatherState> weather = BehaviorProcessor.create();
    private WeatherViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        WeatherRepository repo = HomeFakes.fake(WeatherRepository.class, calls, (name, args) ->
                name.equals("observe") ? weather : null);
        vm = new WeatherViewModel(null, repo);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void startsWithSkeleton_thenFollowsRepository() {
        assertTrue(vm.state.getValue().showSkeleton());
        WeatherState s = new WeatherState();
        s.error = new AppError(AppError.Type.BUSINESS, 0, "未配置和风天气 API Key");
        weather.onNext(s);
        assertTrue(vm.state.getValue().showError());
        assertEquals("未配置和风天气 API Key", vm.state.getValue().errorMessage());
    }

    @Test
    public void refresh_isForced() {
        vm.refresh();
        assertTrue(calls.contains("refresh:true"));
        assertFalse(vm.refreshing.getValue());
    }
}
