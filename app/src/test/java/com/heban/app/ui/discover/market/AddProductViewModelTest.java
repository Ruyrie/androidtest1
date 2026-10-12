package com.heban.app.ui.discover.market;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.model.CropCard;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AddProductViewModelTest {
    private static final long NOW = 1_760_000_000_000L;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MarketFakes.Market market;
    private MarketFakes.Photos photos;
    private BehaviorProcessor<Integer> balance;
    private BehaviorProcessor<List<CropCard>> crops;
    private AddProductViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        market = new MarketFakes.Market();
        photos = new MarketFakes.Photos();
        balance = BehaviorProcessor.createDefault(1280);
        crops = BehaviorProcessor.createDefault(Arrays.asList(
                MarketFakes.crop(4, "草莓", "2号大棚"), MarketFakes.crop(1, "番茄", "1号大棚")));
        vm = new AddProductViewModel(market, MarketFakes.points(balance), MarketFakes.crops(crops),
                MarketFakes.users(MarketFakes.user(1, "老李种菜", "江苏省 句容市 白兔镇")),
                photos, new Gson(), () -> NOW);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private void fillValid() {
        vm.photos.setValue(Arrays.asList("file:///products/1.jpg", "file:///products/2.jpg"));
        vm.title.setValue("句容红颜草莓 当天现摘");
        vm.selectCategory(ProductEntity.CAT_PRODUCE, "水果");
        vm.price.setValue("25");
        vm.minQty.setValue("10");
    }

    @Test
    public void defaults_followPrototype() {
        assertEquals("江苏省 句容市 白兔镇", vm.location.getValue());
        assertEquals("斤", vm.unit.getValue());
        assertEquals(Arrays.asList(AddProductViewModel.WAY_PICKUP, AddProductViewModel.WAY_LOCAL),
                new ArrayList<>(vm.tradeWays.getValue()));
        assertEquals(4, vm.crop.getValue().id);       // 第一种作物
        assertTrue(vm.attachRecord.getValue());
        assertFalse(vm.pin.getValue());
    }

    @Test
    public void validation_showsInlineErrors() {
        vm.publish();
        assertEquals(Integer.valueOf(R.string.add_product_err_photos), vm.photosError.getValue());
        assertEquals(Integer.valueOf(R.string.add_product_err_title), vm.titleError.getValue());
        assertEquals(Integer.valueOf(R.string.add_product_err_category), vm.categoryError.getValue());
        assertEquals(Integer.valueOf(R.string.add_product_err_price), vm.priceError.getValue());
        assertNull(market.published);

        vm.price.setValue("0");
        vm.title.setValue("   ");
        assertFalse(vm.validate());
        assertEquals(Integer.valueOf(R.string.add_product_err_price), vm.priceError.getValue());
        assertEquals(Integer.valueOf(R.string.add_product_err_title), vm.titleError.getValue());

        fillValid();
        assertTrue(vm.validate());
        assertNull(vm.photosError.getValue());
        assertNull(vm.titleError.getValue());
        assertNull(vm.categoryError.getValue());
        assertNull(vm.priceError.getValue());
    }

    @Test
    public void publish_buildsProduct() {
        fillValid();
        vm.toggleTradeWay(AddProductViewModel.WAY_EXPRESS, true);
        vm.toggleTradeWay(AddProductViewModel.WAY_PICKUP, false);
        vm.desc.setValue("大棚种植，膜下滴灌，每天早上现摘，2 斤一盒。");
        vm.publish();

        ProductEntity p = market.published;
        assertEquals("句容红颜草莓 当天现摘", p.title);
        assertEquals(ProductEntity.CAT_PRODUCE, p.category);
        assertEquals("水果", p.subCategory);
        assertEquals(2500, p.priceCent);
        assertEquals("斤", p.unit);
        assertEquals(10.0, p.minQty, 0.0001);
        assertEquals("同城配送、快递", p.tradeWays);
        assertEquals("江苏省 句容市 白兔镇", p.location);
        assertEquals("[\"file:///products/1.jpg\",\"file:///products/2.jpg\"]", p.images);
        assertEquals("[\"同城配送\",\"快递\",\"附种植记录\"]", p.tags);
        assertEquals(Long.valueOf(4), p.cropId);
        assertTrue(p.mine);
        assertEquals("老李种菜", p.sellerName);
        assertEquals("句容白兔镇", p.sellerRegion);
        assertEquals(NOW, p.createdAt);
        assertFalse(market.publishedPin);
        assertEquals(Long.valueOf(99), vm.published.getValue());

        vm.onCleared();                                 // 已发布：保留图片
        assertTrue(photos.deleted.isEmpty());
    }

    @Test
    public void pinSwitch_enabledOnlyWithEnoughPoints() {
        assertTrue(vm.pinEnabled.getValue());
        vm.setPin(true);
        assertTrue(vm.pin.getValue());
        assertEquals(1180, vm.balanceAfterPin());

        balance.onNext(99);                             // 积分不足：关闭并禁用
        assertFalse(vm.pinEnabled.getValue());
        assertFalse(vm.pin.getValue());
        vm.setPin(true);
        assertFalse(vm.pin.getValue());

        balance.onNext(100);                            // 恰好 100 可开启
        assertTrue(vm.pinEnabled.getValue());
        vm.setPin(true);
        fillValid();
        vm.publish();
        assertTrue(market.publishedPin);
    }

    @Test
    public void publish_pointsErrorShowsMessage() {
        fillValid();
        vm.setPin(true);
        market.publishError = MarketFakes.pointsNotEnough();
        vm.publish();
        assertEquals("积分不足", vm.toast.getValue());
        assertFalse(vm.busy.getValue());
        assertNull(vm.published.getValue());
    }

    @Test
    public void noCrops_detachesRecord() {
        crops.onNext(Collections.emptyList());
        assertNull(vm.crop.getValue());
        fillValid();
        vm.publish();
        assertNull(market.published.cropId);
        assertEquals("[\"可自提\",\"同城配送\"]", market.published.tags);
    }

    @Test
    public void photos_limitedToNine_andCoverReorder() {
        List<Uri> uris = new ArrayList<>();
        for (int i = 0; i < 11; i++) uris.add(null);
        vm.addPhotos(uris);
        assertEquals(9, vm.photos.getValue().size());
        assertEquals(Integer.valueOf(9), vm.photoLimit.getValue());
        assertEquals(0, vm.remainingPhotos());

        vm.setCover(2);
        assertEquals("file:///products/3.jpg", vm.photos.getValue().get(0));
        vm.removePhoto(0);
        assertEquals(8, vm.photos.getValue().size());
        assertEquals(Collections.singletonList("file:///products/3.jpg"), photos.deleted);

        vm.onCleared();                                 // 未发布：删除复制的图片
        assertEquals(9, photos.deleted.size());
    }
}
