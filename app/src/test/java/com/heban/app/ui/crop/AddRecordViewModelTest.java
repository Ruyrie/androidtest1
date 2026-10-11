package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.FarmType;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AddRecordViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-10T01:30:45Z"), ZONE);

    private AddCropFakes.Repo repo;
    private AddCropFakes.Photos photos;
    private AddRecordViewModel vm;
    private Boolean savedEvent;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new AddCropFakes.Repo();
        repo.cards = Arrays.asList(AddCropFakes.card(1, "番茄", "樱桃番茄"), AddCropFakes.card(2, "黄瓜", null));
        photos = new AddCropFakes.Photos();
        vm = new AddRecordViewModel(null, repo, photos, new Gson(), CLOCK);
        vm.canSave.observeForever(v -> { });
        vm.fields.observeForever(v -> { });
        vm.costCents.observeForever(v -> { });
        vm.saved.observeForever(v -> savedEvent = v);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void defaults_matchPrototype() {
        vm.start(1, null);
        assertEquals(1, vm.crop.getValue().id);
        assertEquals(FarmType.FERTILIZE, vm.type.getValue());
        assertEquals("kg", vm.unit.getValue());
        assertTrue(vm.sync.getValue());
        assertEquals(LocalDateTime.of(2026, 10, 10, 9, 30), vm.dateTime.getValue());
        assertTrue(vm.canSave.getValue());
        assertTrue(vm.isCropPreset());
    }

    @Test
    public void noPreset_severalCrops_requiresChoice() {
        vm.start(0, null);
        assertNull(vm.crop.getValue());
        assertFalse(vm.canSave.getValue());
        vm.selectCrop(repo.cards.get(1));
        assertTrue(vm.canSave.getValue());
    }

    @Test
    public void noPreset_singleCrop_autoSelected() {
        repo.cards = Collections.singletonList(AddCropFakes.card(5, "草莓", "红颜"));
        vm.start(0, null);
        assertEquals(5, vm.crop.getValue().id);
    }

    @Test
    public void presetType() {
        vm.start(1, "HARVEST");
        assertEquals(FarmType.HARVEST, vm.type.getValue());
        assertEquals("kg", vm.unit.getValue());
        assertFalse(vm.fields.getValue().hasMaterial());
    }

    @Test
    public void switchingType_keepsDraftPerType() {
        vm.start(1, null);
        vm.material.setValue("尿素");
        vm.amount.setValue("5");
        vm.selectType(FarmType.SPRAY);
        assertEquals("", vm.material.getValue());
        assertEquals("mL", vm.unit.getValue());
        vm.material.setValue("百菌清");
        vm.selectType(FarmType.FERTILIZE);
        assertEquals("尿素", vm.material.getValue());
        assertEquals("5", vm.amount.getValue());
        assertEquals("kg", vm.unit.getValue());
        vm.selectType(FarmType.SPRAY);
        assertEquals("百菌清", vm.material.getValue());
    }

    @Test
    public void save_fertilize_buildsRecordAndSyncs() {
        vm.start(1, null);
        vm.material.setValue("硫酸钾（K₂O ≥ 50%）");
        vm.amount.setValue("5");
        vm.cost.setValue("60");
        vm.addPhotos(Arrays.asList((Uri) null, null));
        vm.note.setValue(" ");
        assertEquals(6000L, (long) vm.costCents.getValue());
        vm.save();

        assertEquals(1, repo.savedRecords.size());
        FarmRecordEntity r = repo.savedRecords.get(0);
        assertEquals(1, r.cropId);
        assertEquals("FERTILIZE", r.type);
        assertEquals("硫酸钾（K₂O ≥ 50%）", r.material);
        assertEquals(5.0, r.amount, 1e-9);
        assertEquals("kg", r.unit);
        assertEquals(6000, r.costCent);
        assertEquals(LocalDateTime.of(2026, 10, 10, 9, 30).atZone(ZONE).toInstant().toEpochMilli(), r.date);
        assertEquals("[\"file:///copied/records/1.jpg\",\"file:///copied/records/2.jpg\"]", r.photos);
        assertNull(r.note);
        assertTrue(repo.syncFlags.get(0));
        assertEquals(Boolean.TRUE, savedEvent);
        vm.onCleared();
        assertTrue(photos.deleted.isEmpty());   // 已保存的照片不删除
    }

    @Test
    public void save_withoutCost_notReportedAsSynced() {
        vm.start(1, "PRUNE");
        vm.material.setValue("整枝打杈");
        vm.sync.setValue(true);
        vm.save();
        FarmRecordEntity r = repo.savedRecords.get(0);
        assertEquals("PRUNE", r.type);
        assertNull(r.unit);
        assertEquals(0, r.costCent);
        assertNull(r.photos);
        assertEquals(Boolean.FALSE, savedEvent);
    }

    @Test
    public void save_syncOff() {
        vm.start(1, null);
        vm.material.setValue("尿素");
        vm.cost.setValue("12.5");
        vm.sync.setValue(false);
        vm.save();
        assertFalse(repo.syncFlags.get(0));
        assertEquals(1250, repo.savedRecords.get(0).costCent);
        assertEquals(Boolean.FALSE, savedEvent);
    }

    @Test
    public void save_validation() {
        vm.start(1, null);
        vm.amount.setValue("0");
        vm.cost.setValue("1.234");
        vm.save();
        assertEquals(R.string.add_record_error_required, (int) vm.materialError.getValue());
        assertEquals(R.string.add_record_error_positive, (int) vm.amountError.getValue());
        assertEquals(R.string.add_record_error_cost_decimals, (int) vm.costError.getValue());
        assertTrue(repo.savedRecords.isEmpty());

        vm.material.setValue("尿素");
        assertEquals(0, (int) vm.materialError.getValue());
        vm.amount.setValue("abc.");
        vm.cost.setValue(".");
        vm.save();
        assertEquals(R.string.add_record_error_number, (int) vm.amountError.getValue());
        assertEquals(R.string.add_record_error_cost, (int) vm.costError.getValue());
    }

    @Test
    public void save_harvest_requiresAmount() {
        vm.start(1, "HARVEST");
        vm.save();
        assertEquals(R.string.add_record_error_required, (int) vm.amountError.getValue());
        vm.amount.setValue("60");
        vm.unit.setValue("斤");
        vm.save();
        FarmRecordEntity r = repo.savedRecords.get(0);
        assertNull(r.material);
        assertEquals(60.0, r.amount, 1e-9);
        assertEquals("斤", r.unit);
    }

    @Test
    public void photos_limitedToNine() {
        vm.start(1, null);
        final Integer[] limit = {null};
        vm.photoLimit.observeForever(v -> limit[0] = v);
        List<Uri> seven = Collections.nCopies(7, null);
        vm.addPhotos(seven);
        assertEquals(7, vm.photos.getValue().size());
        assertNull(limit[0]);
        vm.addPhotos(Collections.nCopies(5, null));
        assertEquals(9, vm.photos.getValue().size());
        assertEquals(Integer.valueOf(9), limit[0]);
        assertEquals(0, vm.remainingPhotos());

        String first = vm.photos.getValue().get(0);
        vm.removePhoto(first);
        assertEquals(8, vm.photos.getValue().size());
        assertEquals(Collections.singletonList(first), photos.deleted);
    }

    @Test
    public void leavingWithoutSave_deletesCopiedPhotos() {
        vm.start(1, null);
        vm.addPhotos(Arrays.asList((Uri) null, null));
        vm.onCleared();
        assertEquals(2, photos.deleted.size());
    }

    @Test
    public void copyFailure_showsMessage() {
        photos.fail = true;
        vm.start(1, null);
        final Integer[] msg = {null};
        vm.messageRes.observeForever(v -> msg[0] = v);
        vm.addPhotos(Collections.singletonList(null));
        assertEquals(Integer.valueOf(R.string.add_crop_photo_failed), msg[0]);
        assertFalse(vm.photoBusy.getValue());
        assertTrue(vm.photos.getValue().isEmpty());
    }
}
