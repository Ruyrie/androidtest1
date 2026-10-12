package com.heban.app.ui.discover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.local.entity.UserEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DiscoverViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    private DiscoverFakes.Pests pests;
    private DiscoverFakes.Market market;
    private DiscoverFakes.Ledger ledger;
    private BehaviorProcessor<Long> userId;
    private PublishProcessor<UserEntity> user;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        pests = new DiscoverFakes.Pests();
        market = new DiscoverFakes.Market();
        ledger = new DiscoverFakes.Ledger();
        userId = BehaviorProcessor.createDefault(0L);
        user = PublishProcessor.create();
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private DiscoverViewModel create() {
        return new DiscoverViewModel(null, pests, market, ledger, userId, user, TODAY);
    }

    private static UserEntity user(String region) {
        UserEntity u = new UserEntity();
        u.id = 1;
        u.region = region;
        return u;
    }

    @Test
    public void counts_fromRepositories() {
        pests.count.onNext(128);
        market.count.onNext(36);
        DiscoverViewModel vm = create();
        assertEquals(128, (int) vm.pestCount.getValue());
        assertEquals(36, (int) vm.marketCount.getValue());
        assertNull("D01 在售数不按类别过滤", market.countCategory);

        pests.count.onNext(130);
        assertEquals(130, (int) vm.pestCount.getValue());
    }

    @Test
    public void guest_noBalance_monthOnlyHeader() {
        DiscoverViewModel vm = create();
        assertFalse(vm.signedIn.getValue());
        assertNull(vm.balance.getValue());
        assertNull("游客不查询记账", ledger.requested);
        assertEquals("10 月", vm.seasonHeader.getValue());
    }

    @Test
    public void signedIn_balanceOfCurrentMonth() {
        userId.onNext(1L);
        DiscoverViewModel vm = create();
        assertEquals(YearMonth.of(2026, 10), ledger.requested);

        ledger.month.onNext(DiscoverFakes.Ledger.summary(520_000, 174_000));
        assertTrue(vm.signedIn.getValue());
        assertEquals("+¥3,460", vm.balance.getValue());

        ledger.month.onNext(DiscoverFakes.Ledger.summary(10_000, 130_050));
        assertEquals("-¥1,200.5", vm.balance.getValue());

        ledger.month.onNext(DiscoverFakes.Ledger.summary(0, 0));
        assertEquals("¥0", vm.balance.getValue());
    }

    @Test
    public void logout_switchesBackToGuest() {
        userId.onNext(1L);
        DiscoverViewModel vm = create();
        ledger.month.onNext(DiscoverFakes.Ledger.summary(100, 0));
        assertEquals("+¥1", vm.balance.getValue());

        userId.onNext(0L);
        assertFalse(vm.signedIn.getValue());
        assertNull(vm.balance.getValue());
    }

    @Test
    public void seasonHeader_usesRegionOfSignedInUser() {
        userId.onNext(1L);
        DiscoverViewModel vm = create();
        assertEquals("10 月", vm.seasonHeader.getValue());

        user.onNext(user("江苏省 南京市 江宁区"));
        assertEquals("江宁区 · 10 月", vm.seasonHeader.getValue());

        user.onNext(user(null));
        assertEquals("没有地区时只显示月份", "10 月", vm.seasonHeader.getValue());
    }

    @Test
    public void seasonal_queriesCurrentMonth_andEmptyState() {
        DiscoverViewModel vm = create();
        assertEquals(10, pests.seasonalMonth);
        assertTrue(vm.seasonalEmpty.getValue());

        PestEntity a = DiscoverFakes.pest(1, "霜霉病", "病害", "卵菌病害", "黄瓜、白菜", "高发");
        PestEntity b = DiscoverFakes.pest(2, "灰霉病", "病害", "真菌病害", "番茄、草莓", "较高");
        pests.seasonal.onNext(Arrays.asList(a, b));
        assertFalse(vm.seasonalEmpty.getValue());
        assertEquals(2, vm.seasonal.getValue().size());
    }

    @Test
    public void seasonalEmpty_falseUntilLoaded() {
        BehaviorProcessor<java.util.List<PestEntity>> never = BehaviorProcessor.create();
        DiscoverFakes.Pests slow = new DiscoverFakes.Pests() {
            @Override
            public io.reactivex.rxjava3.core.Flowable<java.util.List<PestEntity>> seasonal(int month) {
                return never;
            }
        };
        DiscoverViewModel vm = new DiscoverViewModel(null, slow, market, ledger, userId, user, TODAY);
        assertFalse("加载前不显示空状态", vm.seasonalEmpty.getValue());
    }

    @Test
    public void nearby_firstTwo_andEmptyState() {
        DiscoverViewModel vm = create();
        assertEquals(2, market.nearbyLimit);
        assertTrue(vm.nearbyEmpty.getValue());

        ProductEntity p1 = DiscoverFakes.product(1, "句容红颜草莓 现摘", 2_500, "斤", "草莓姐姐", 12);
        ProductEntity p2 = DiscoverFakes.product(2, "小型微耕机 九成新", 180_000, null, "果园阿强", 6);
        ProductEntity p3 = DiscoverFakes.product(3, "有机肥", 3_800, "袋", "老王", 20);
        market.nearby.onNext(Arrays.asList(p1, p2, p3));
        assertFalse(vm.nearbyEmpty.getValue());
        assertEquals(2, vm.nearby.getValue().size());
        assertEquals(1, vm.nearby.getValue().get(0).id);

        market.nearby.onNext(Collections.emptyList());
        assertTrue(vm.nearbyEmpty.getValue());
    }

    @Test
    public void error_showsToast() {
        DiscoverFakes.Pests failing = new DiscoverFakes.Pests() {
            @Override
            public io.reactivex.rxjava3.core.Flowable<Integer> count() {
                return io.reactivex.rxjava3.core.Flowable.error(
                        new com.heban.app.data.remote.BusinessException("X", "读取失败"));
            }
        };
        DiscoverViewModel vm = new DiscoverViewModel(null, failing, market, ledger, userId, user, TODAY);
        assertEquals("读取失败", vm.toast.getValue());
    }
}
