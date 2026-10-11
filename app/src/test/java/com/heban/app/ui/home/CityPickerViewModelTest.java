package com.heban.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.data.model.CityOption;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.repository.WeatherRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;

public class CityPickerViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final List<String> calls = new ArrayList<>();
    private final TestScheduler scheduler = new TestScheduler();
    private Single<List<CityOption>> searchResult;
    private Completable setLocationResult = Completable.complete();
    private CityPickerViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        searchResult = Single.just(Collections.singletonList(city("101190104", "江宁")));
        WeatherRepository repo = HomeFakes.fake(WeatherRepository.class, calls, (name, args) -> {
            if (name.equals("searchCity")) return searchResult;
            if (name.equals("setLocation")) return setLocationResult;
            return null;
        });
        vm = new CityPickerViewModel(null, repo, scheduler);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private static CityOption city(String id, String name) {
        CityOption c = new CityOption();
        c.id = id;
        c.name = name;
        c.adm2 = "南京";
        c.adm1 = "江苏";
        return c;
    }

    private long searches() {
        return calls.stream().filter(c -> c.startsWith("searchCity")).count();
    }

    @Test
    public void search_isDebounced300ms() {
        vm.onQueryChanged("江");
        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
        vm.onQueryChanged("江宁");
        scheduler.advanceTimeBy(299, TimeUnit.MILLISECONDS);
        assertEquals(0, searches());
        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        assertEquals(1, searches());
        assertTrue(calls.contains("searchCity:江宁"));
        assertEquals(CityPickerViewModel.Status.RESULTS, vm.status.getValue());
        assertEquals(1, vm.results.getValue().size());
    }

    @Test
    public void sameQuery_notSearchedTwice() {
        vm.onQueryChanged("江宁");
        scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        vm.onQueryChanged(" 江宁 ");
        scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertEquals(1, searches());
    }

    @Test
    public void emptyQuery_backToIdle() {
        vm.onQueryChanged("江宁");
        scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        vm.onQueryChanged("");
        scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertEquals(CityPickerViewModel.Status.IDLE, vm.status.getValue());
        assertTrue(vm.results.getValue().isEmpty());
        assertEquals(1, searches());
    }

    @Test
    public void noResults_empty() {
        searchResult = Single.just(Collections.emptyList());
        vm.onQueryChanged("火星");
        scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertEquals(CityPickerViewModel.Status.EMPTY, vm.status.getValue());
    }

    @Test
    public void searchError_showsMessageAndKeepsListening() {
        searchResult = Single.error(new IOException("x"));
        vm.onQueryChanged("江宁");
        scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertEquals(CityPickerViewModel.Status.ERROR, vm.status.getValue());
        assertEquals("网络不太好，检查后再试一次", vm.errorText.getValue());

        searchResult = Single.just(Collections.singletonList(city("1", "江浦")));
        vm.onQueryChanged("江浦");
        scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertEquals(CityPickerViewModel.Status.RESULTS, vm.status.getValue());
    }

    @Test
    public void select_savesLocationAndCloses() {
        vm.select(city("101190104", "江宁"));
        assertEquals(1, calls.stream().filter(c -> c.startsWith("setLocation")).count());
        assertTrue(vm.done.getValue());
        assertFalse(vm.saving.getValue());
    }

    @Test
    public void select_errorShowsToast() {
        setLocationResult = Completable.error(new BusinessException("X", "保存失败"));
        vm.select(city("101190104", "江宁"));
        assertEquals("保存失败", vm.toast.getValue());
        assertFalse(vm.saving.getValue());
    }

    @Test
    public void region_secondLine() {
        CityOption c = city("1", "江宁");
        assertEquals("江苏 南京", CityPickerAdapter.region(c));
        c.adm2 = "江苏";
        assertEquals("江苏", CityPickerAdapter.region(c));
    }
}
