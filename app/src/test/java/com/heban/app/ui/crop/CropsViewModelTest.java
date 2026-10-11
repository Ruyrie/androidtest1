package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.repository.CropRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;

public class CropsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CropsFakeRepository repo;
    private TestScheduler debounce;
    private CropsViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new CropsFakeRepository();
        debounce = new TestScheduler();
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private void sample() {
        repo.cards.onNext(Arrays.asList(
                CropsFakeRepository.card(1, "番茄", "樱桃番茄", "1号大棚", 0.5, "大棚", "结果期", 68, 110, null),
                CropsFakeRepository.card(2, "黄瓜", null, "2号大棚", 0.4, "大棚", "开花期", 35, 78, "霜霉病风险"),
                CropsFakeRepository.card(3, "冬小麦", null, "东地块", 3.0, "露天", "分蘖期", 21, 240, null),
                CropsFakeRepository.card(4, "草莓", "红颜", "2号大棚", 0.3, "大棚", "定植期", 3, 150, null)));
        CropStats s = new CropStats();
        s.growing = 4;
        s.weekTasks = 12;
        s.attention = 1;
        repo.stats.onNext(s);
    }

    private static long[] ids(List<CropCard> list) {
        long[] out = new long[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i).id;
        return out;
    }

    @Test
    public void noCrops_showsEmptyState() {
        vm = new CropsViewModel(null, repo, debounce);
        assertFalse(vm.hasCrops.getValue());
        assertTrue(vm.showEmpty.getValue());
        assertFalse(vm.showList.getValue());
        assertFalse(vm.showNoMatch.getValue());
        assertEquals(0, (int) vm.totalCount.getValue());
    }

    @Test
    public void loadsCardsStatsAndTotal() {
        sample();
        vm = new CropsViewModel(null, repo, debounce);
        assertEquals(4, vm.cards.getValue().size());
        assertEquals(4, (int) vm.totalCount.getValue());
        assertEquals(12, vm.stats.getValue().weekTasks);
        assertTrue(vm.showList.getValue());
        assertFalse(vm.showEmpty.getValue());
    }

    @Test
    public void filterSwitchesRepositoryQuery() {
        sample();
        vm = new CropsViewModel(null, repo, debounce);
        vm.selectFilter(CropRepository.FILTER_OPEN);
        assertTrue(repo.filterQueries.contains(CropRepository.FILTER_OPEN));
        assertEquals(1, vm.cards.getValue().size());
        assertEquals(3, vm.cards.getValue().get(0).id);

        vm.selectFilter(CropRepository.FILTER_ATTENTION);
        assertEquals(2, vm.cards.getValue().get(0).id);
        // 「全部 N」始终是全部作物数
        assertEquals(4, (int) vm.totalCount.getValue());
        assertEquals(CropRepository.FILTER_ATTENTION, vm.selectedFilter.getValue());
    }

    @Test
    public void searchIsDebouncedAndMatchesNameVarietyPlot() {
        sample();
        vm = new CropsViewModel(null, repo, debounce);
        vm.hasQuery.observeForever(v -> { });
        vm.search.setValue("2号");
        assertEquals(4, vm.cards.getValue().size());   // 300ms 内不生效
        debounce.advanceTimeBy(CropsViewModel.SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        assertEquals(2, vm.cards.getValue().size());

        vm.search.setValue("红颜");
        debounce.advanceTimeBy(CropsViewModel.SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        assertEquals(1, vm.cards.getValue().size());
        assertEquals(4, vm.cards.getValue().get(0).id);

        vm.clearSearch();                               // 清空立即生效
        assertEquals(4, vm.cards.getValue().size());
        assertFalse(vm.hasQuery.getValue());
    }

    @Test
    public void searchWithoutMatch_showsNoMatchNotEmpty() {
        sample();
        vm = new CropsViewModel(null, repo, debounce);
        vm.hasQuery.observeForever(v -> { });
        vm.search.setValue("玉米");
        debounce.advanceTimeBy(CropsViewModel.SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        assertTrue(vm.showNoMatch.getValue());
        assertFalse(vm.showEmpty.getValue());
        assertFalse(vm.showList.getValue());
        assertTrue(vm.hasCrops.getValue());
        assertTrue(vm.hasQuery.getValue());
    }

    @Test
    public void searchAppliesToFilteredList() {
        sample();
        vm = new CropsViewModel(null, repo, debounce);
        vm.search.setValue("大棚");
        debounce.advanceTimeBy(CropsViewModel.SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        assertEquals(3, vm.cards.getValue().size());
        vm.selectFilter(CropRepository.FILTER_ATTENTION);
        assertEquals(1, ids(vm.cards.getValue()).length);
        assertEquals(2, vm.cards.getValue().get(0).id);
    }
}
