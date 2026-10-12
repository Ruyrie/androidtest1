package com.heban.app.ui.discover.market;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.ProductEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ProductViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MarketFakes.Market market;
    private MarketFakes.Favorites favorites;
    private ProductViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        market = new MarketFakes.Market();
        favorites = new MarketFakes.Favorites();
        vm = new ProductViewModel(market, favorites, new Gson());
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private static ProductEntity strawberry() {
        ProductEntity p = MarketFakes.product(2, ProductEntity.CAT_PRODUCE, "句容红颜草莓 当天现摘", 2500, "斤");
        p.wants = 23;
        p.views = 418;
        p.images = "[\"images/products/strawberry_fresh.jpg\",\"b.jpg\",\"c.jpg\",\"d.jpg\"]";
        p.tags = "[\"产地直供\",\"可自提\",\"同城配送\",\"附种植记录\"]";
        return p;
    }

    @Test
    public void wants_addsOneWhenFavorited() {
        vm.start(2);
        market.product.onNext(strawberry());
        assertEquals(Integer.valueOf(23), vm.wants.getValue());
        assertFalse(vm.favorite.getValue());

        vm.toggleFavorite();
        assertTrue(vm.favorite.getValue());
        assertEquals(Integer.valueOf(24), vm.wants.getValue());

        vm.toggleFavorite();
        assertEquals(Integer.valueOf(23), vm.wants.getValue());
    }

    @Test
    public void parsesImagesAndFeatures() {
        vm.start(2);
        market.product.onNext(strawberry());
        assertEquals(4, vm.images.getValue().size());
        assertEquals(Arrays.asList("产地直供", "可自提", "同城配送", "附种植记录"), vm.features.getValue());
        assertFalse(vm.mine.getValue());
    }

    @Test
    public void addView_onlyOncePerOpen() {
        vm.start(2);
        vm.start(2);                    // 重建界面时再次调用
        assertEquals(1, market.views);
    }

    @Test
    public void delete_onlyOwnProduct() {
        vm.start(2);
        market.product.onNext(strawberry());
        vm.delete();
        assertEquals(0, market.deleted);

        ProductEntity mine = strawberry();
        mine.mine = true;
        market.product.onNext(mine);
        vm.delete();
        assertEquals(2, market.deleted);
        assertTrue(vm.deleted.getValue());
    }
}
