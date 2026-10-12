package com.heban.app.ui.discover.pest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.ui.discover.DiscoverFakes;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;

public class PestsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private DiscoverFakes.Pests repo;
    private TestScheduler debounce;
    private PestsViewModel vm;

    private final PestEntity cucumber = DiscoverFakes.pest(1, "黄瓜霜霉病", "病害", "卵菌病害", "黄瓜", "高发");
    private final PestEntity tomato = DiscoverFakes.pest(2, "番茄灰霉病", "病害", "真菌病害", "番茄", "较高");
    private final PestEntity aphid = DiscoverFakes.pest(4, "蚜虫", "虫害", "虫害", "多种蔬菜", "较高");

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new DiscoverFakes.Pests();
        repo.crops = Arrays.asList("黄瓜", "番茄", "白菜");
        debounce = new TestScheduler();
        vm = new PestsViewModel(null, repo, debounce, LocalDate.of(2026, 10, 11));
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private void type(String text) {
        vm.search.setValue(text);
    }

    @Test
    public void initialQuery_unfilteredImmediately() {
        assertEquals(1, repo.calls.size());
        DiscoverFakes.Pests.Call c = repo.last();
        assertNull(c.keyword);
        assertNull(c.kind);
        assertNull(c.crop);
        assertEquals(Arrays.asList("黄瓜", "番茄", "白菜"), vm.cropOptions.getValue());
    }

    @Test
    public void results_countAndNoResult() {
        assertFalse("查询完成前不显示无结果", vm.showNoResult.getValue());
        repo.last().results.onNext(Arrays.asList(cucumber, tomato, aphid));
        assertEquals(3, vm.pests.getValue().size());
        assertEquals(3, (int) vm.resultCount.getValue());
        assertFalse(vm.showNoResult.getValue());

        repo.last().results.onNext(Collections.emptyList());
        assertEquals(0, (int) vm.resultCount.getValue());
        assertTrue(vm.showNoResult.getValue());
        assertFalse("未筛选时为空：百科本身没有内容", vm.filtered.getValue());
    }

    @Test
    public void search_debounced300ms() {
        type("霜");
        debounce.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        type("霜霉");
        debounce.advanceTimeBy(299, TimeUnit.MILLISECONDS);
        assertEquals("停顿不足 300ms 不查询", 1, repo.calls.size());

        debounce.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        assertEquals(2, repo.calls.size());
        assertEquals("霜霉", repo.last().keyword);
    }

    @Test
    public void search_trimmed_andDistinct() {
        type(" 蚜虫 ");
        debounce.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertEquals("蚜虫", repo.last().keyword);
        int calls = repo.calls.size();

        type("蚜虫");
        debounce.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertEquals("关键字没变不重复查询", calls, repo.calls.size());
    }

    @Test
    public void search_switchMapCancelsPreviousQuery() {
        DiscoverFakes.Pests.Call first = repo.last();
        type("蚜");
        debounce.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertTrue(first.cancelled);
        DiscoverFakes.Pests.Call second = repo.last();

        // 旧查询结果不应再到达界面
        first.results.onNext(Arrays.asList(cucumber, tomato));
        second.results.onNext(Collections.singletonList(aphid));
        assertEquals(Collections.singletonList(aphid), vm.pests.getValue());
    }

    @Test
    public void clearSearch_appliesImmediately() {
        type("番茄");
        debounce.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        assertEquals("番茄", repo.last().keyword);

        vm.clearSearch();
        assertNull("清空不等待 debounce", repo.last().keyword);
    }

    @Test
    public void filters_combineWithKeyword() {
        vm.selectKind(PestEntity.KIND_INSECT);
        assertEquals("虫害", repo.last().kind);
        assertNull(repo.last().crop);

        vm.selectCrop("黄瓜");
        assertEquals("虫害", repo.last().kind);
        assertEquals("黄瓜", repo.last().crop);
        assertEquals("黄瓜", vm.selectedCrop.getValue());

        type("霜霉");
        debounce.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        DiscoverFakes.Pests.Call c = repo.last();
        assertEquals("霜霉", c.keyword);
        assertEquals("虫害", c.kind);
        assertEquals("黄瓜", c.crop);

        c.results.onNext(Collections.emptyList());
        assertTrue(vm.showNoResult.getValue());
        assertTrue("有筛选条件：没有找到相关病虫害", vm.filtered.getValue());

        vm.selectKind(PestsViewModel.KIND_ALL);
        vm.selectCrop(null);
        assertNull(repo.last().kind);
        assertNull(repo.last().crop);
        assertNull(vm.selectedCrop.getValue());
        assertEquals("霜霉", repo.last().keyword);
    }

    @Test
    public void sameFilter_doesNotRequery() {
        int calls = repo.calls.size();
        vm.selectKind(PestsViewModel.KIND_ALL);
        vm.selectCrop(null);
        vm.selectCrop("  ");
        assertEquals(calls, repo.calls.size());

        vm.selectKind(PestEntity.KIND_DISEASE);
        vm.selectKind(PestEntity.KIND_DISEASE);
        assertEquals(calls + 1, repo.calls.size());
        assertEquals("病害", vm.selectedKind.getValue());
    }

    @Test
    public void banner_firstSeasonal_hiddenWhileSearching() {
        assertEquals(10, repo.seasonalMonth);
        assertFalse(vm.showBanner.getValue());

        PestEntity downy = DiscoverFakes.pest(10, "霜霉病", "病害", "卵菌病害", "黄瓜、白菜", "高发");
        repo.seasonal.onNext(Arrays.asList(downy, tomato));
        assertEquals(downy, vm.banner.getValue());
        assertTrue(vm.showBanner.getValue());

        type("蚜虫");
        debounce.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        repo.last().results.onNext(Collections.singletonList(aphid));
        assertFalse("输入关键字搜索时隐藏横幅", vm.showBanner.getValue());

        vm.clearSearch();
        repo.last().results.onNext(Arrays.asList(cucumber, aphid));
        assertTrue(vm.showBanner.getValue());

        vm.selectKind(PestEntity.KIND_INSECT);
        repo.last().results.onNext(Collections.singletonList(aphid));
        assertTrue("类别筛选时保留横幅", vm.showBanner.getValue());

        repo.seasonal.onNext(Collections.emptyList());
        assertNull(vm.banner.getValue());
        assertFalse(vm.showBanner.getValue());
    }

    @Test
    public void focusRequest_onlyOnce() {
        assertFalse(vm.consumeFocusRequest(false));
        assertTrue(vm.consumeFocusRequest(true));
        assertFalse("返回本页时不再弹出键盘", vm.consumeFocusRequest(true));
    }

    @Test
    public void resultsOrderKept() {
        List<PestEntity> list = Arrays.asList(aphid, cucumber, tomato);
        repo.last().results.onNext(list);
        assertEquals(list, vm.pests.getValue());
    }
}
