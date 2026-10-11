package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.FarmType;

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

public class CropDetailViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CropsFakeRepository repo;
    private CropDetailViewModel vm;
    private final List<Integer> toasts = new ArrayList<>();
    private int closed;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new CropsFakeRepository();
        vm = new CropDetailViewModel(null, repo, new Gson());
        vm.toastRes.observeForever(toasts::add);
        vm.close.observeForever(c -> closed++);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private static FarmRecordEntity record(long id, String type, long date, long cost, String photos) {
        FarmRecordEntity r = new FarmRecordEntity();
        r.id = id;
        r.type = type;
        r.date = date;
        r.costCent = cost;
        r.photos = photos;
        return r;
    }

    private CropDetail detail() {
        CropDetail d = new CropDetail();
        d.crop = new CropEntity();
        d.crop.id = 7;
        d.crop.name = "番茄";
        d.records.add(record(1, "FERTILIZE", 300, 5000, "[\"p1.jpg\",\"p2.jpg\"]"));
        d.records.add(record(2, "SPRAY", 200, 0, null));
        d.records.add(record(3, "FERTILIZE", 100, 2000, "[\"p3.jpg\"]"));
        return d;
    }

    @Test
    public void derivesPhotosAndTypeStats() {
        BehaviorProcessor<CropDetail> p = BehaviorProcessor.createDefault(detail());
        repo.details.put(7L, p);
        vm.start(7);
        assertEquals(3, vm.records.getValue().size());
        assertEquals(3, vm.photos.getValue().size());
        assertEquals("p1.jpg", vm.photos.getValue().get(0));
        List<CropDetailViewModel.TypeStat> stats = vm.typeStats.getValue();
        assertEquals(2, stats.size());
        assertEquals(FarmType.FERTILIZE, stats.get(0).type);
        assertEquals(2, stats.get(0).count);
        assertEquals(7000, stats.get(0).costCent);
        assertEquals(300, stats.get(0).lastAt);
        assertEquals(FarmType.SPRAY, stats.get(1).type);
        assertEquals(7000L, (long) vm.totalCost.getValue());
        assertTrue(vm.loaded.getValue());
    }

    @Test
    public void missingCrop_closesWithMessage() {
        vm.start(42);
        assertEquals(1, closed);
        assertEquals(Integer.valueOf(R.string.crop_detail_not_found), toasts.get(0));
    }

    @Test
    public void deleteCrop_closesOnce() {
        repo.details.put(7L, BehaviorProcessor.createDefault(detail()));
        vm.start(7);
        vm.deleteCrop();
        assertEquals(1, closed);
        assertEquals(7L, (long) repo.deletedCrops.get(0));
        assertTrue(toasts.isEmpty());
    }

    @Test
    public void dismissAttentionAndDeleteRecord() {
        repo.details.put(7L, BehaviorProcessor.createDefault(detail()));
        vm.start(7);
        vm.dismissAttention();
        vm.deleteRecord(2);
        assertEquals(7L, (long) repo.dismissed.get(0));
        assertEquals(2L, (long) repo.deletedRecords.get(0));
        assertEquals(Integer.valueOf(R.string.crop_detail_attention_dismissed), toasts.get(0));
        assertEquals(Integer.valueOf(R.string.crop_detail_record_deleted), toasts.get(1));
    }

    @Test
    public void selectTab() {
        vm.selectTab(CropDetailViewModel.TAB_PHOTOS);
        assertEquals(CropDetailViewModel.TAB_PHOTOS, (int) vm.tab.getValue());
    }
}
