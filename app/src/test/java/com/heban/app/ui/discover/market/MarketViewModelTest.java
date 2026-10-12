package com.heban.app.ui.discover.market;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.repository.MarketRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class MarketViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MarketFakes.Market market;
    private MarketFakes.Favorites favorites;
    private TestScheduler debounce;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(s -> Schedulers.trampoline());
        market = new MarketFakes.Market();
        favorites = new MarketFakes.Favorites();
        debounce = new TestScheduler();
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private MarketViewModel vm(String region) {
        return new MarketViewModel(market, favorites,
                MarketFakes.users(region == null ? null : MarketFakes.user(1, "老李种菜", region)),
                debounce, src -> src);
    }

    @Test
    public void defaultQuery_allCategoriesByDistance() {
        MarketViewModel vm = vm("江苏省 南京市 江宁区");
        TestSubscriber<?> ts = vm.products.test();
        ts.assertValueCount(1);
        assertEquals(Arrays.asList("null|" + MarketRepository.SORT_DISTANCE), market.calls);
        assertEquals(Integer.valueOf(36), vm.count.getValue());
        assertEquals("江宁区", vm.district.getValue());
        ts.cancel();
    }

    @Test
    public void switchingCategoryAndSort_requeriesRepository() {
        MarketViewModel vm = vm(null);
        TestSubscriber<?> ts = vm.products.test();

        vm.selectCategory(ProductEntity.CAT_SUPPLY);
        vm.selectSort(MarketRepository.SORT_PRICE);
        vm.selectCategory(MarketViewModel.CAT_ALL);
        vm.selectSort(MarketRepository.SORT_PRICE);          // 同一排序不重复查询

        assertEquals(Arrays.asList(
                "null|" + MarketRepository.SORT_DISTANCE,
                "农资|" + MarketRepository.SORT_DISTANCE,
                "农资|" + MarketRepository.SORT_PRICE,
                "null|" + MarketRepository.SORT_PRICE), market.calls);
        assertEquals(MarketViewModel.CAT_ALL, vm.category.getValue());
        assertEquals(MarketRepository.SORT_PRICE, vm.sort.getValue());
        ts.assertValueCount(4);
        ts.cancel();
    }

    @Test
    public void count_followsCategoryOnly() {
        MarketViewModel vm = vm(null);
        vm.selectCategory(ProductEntity.CAT_SEEDLING);
        assertEquals(Integer.valueOf(2), vm.count.getValue());
        vm.selectSort(MarketRepository.SORT_NEWEST);           // 排序不影响数量
        assertEquals(Arrays.asList("null", "种苗"), market.countCalls);
        assertNull(vm.district.getValue());                    // 游客：没有地区
    }

    @Test
    public void search_filtersWithoutRequerying() {
        MarketViewModel vm = vm(null);
        TestSubscriber<?> ts = vm.products.test();
        vm.openSearch();
        assertTrue(vm.searchOpen.getValue());

        vm.search.setValue("草莓");
        ts.assertValueCount(1);                                // 防抖中
        debounce.advanceTimeBy(MarketViewModel.SEARCH_DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        ts.assertValueCount(2);                                // 新的过滤结果
        assertEquals(1, market.calls.size());                  // 没有重新查询
        assertTrue(vm.hasKeyword());

        vm.closeSearch();
        assertFalse(vm.searchOpen.getValue());
        assertEquals("", vm.search.getValue());
        ts.assertValueCount(3);                                // 清空立即生效
        ts.cancel();
    }

    @Test
    public void favoritesOnly_toggles() {
        MarketViewModel vm = vm(null);
        TestSubscriber<?> ts = vm.products.test();
        vm.toggleFavoritesOnly();
        assertTrue(vm.favoritesOnlyOn.getValue());
        ts.assertValueCount(2);
        vm.toggleFavoritesOnly();
        assertFalse(vm.favoritesOnlyOn.getValue());
        ts.assertValueCount(3);
        assertEquals(1, market.calls.size());
        ts.cancel();
    }

    @Test
    public void filter_matchesKeywordAndFavorites() {
        ProductEntity strawberry = MarketFakes.product(2, ProductEntity.CAT_PRODUCE, "句容红颜草莓 当天现摘", 2500, "斤");
        ProductEntity tiller = MarketFakes.product(3, ProductEntity.CAT_MACHINE, "小型微耕机 九成新", 180000, null);
        tiller.sellerName = "果园阿强";

        MarketViewModel.Filter any = new MarketViewModel.Filter("", MarketViewModel.Filter.ANY_IDS);
        assertTrue(any.test(strawberry));
        assertTrue(any.test(tiller));

        MarketViewModel.Filter word = new MarketViewModel.Filter("阿强", MarketViewModel.Filter.ANY_IDS);
        assertFalse(word.test(strawberry));
        assertTrue(word.test(tiller));

        MarketViewModel.Filter fav = new MarketViewModel.Filter("", new java.util.HashSet<>(Arrays.asList(2L)));
        assertTrue(fav.test(strawberry));
        assertFalse(fav.test(tiller));
    }
}
