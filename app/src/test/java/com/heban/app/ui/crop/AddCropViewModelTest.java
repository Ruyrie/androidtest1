package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.local.entity.CropEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AddCropViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** 2026-10-10 09:00 北京时间 */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-10T01:00:00Z"), ZONE);

    private AddCropFakes.Repo repo;
    private AddCropFakes.Photos photos;
    private AddCropViewModel vm;
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
        repo.plots = Arrays.asList(AddCropFakes.plot(1, "1号大棚", "大棚"),
                AddCropFakes.plot(2, "2号大棚", "大棚"), AddCropFakes.plot(3, "东头露天地", "露天"));
        photos = new AddCropFakes.Photos();
        vm = new AddCropViewModel(null, repo, photos, CLOCK);
        vm.canSave.observeForever(v -> { });
        vm.expectedDays.observeForever(v -> { });
        vm.plotName.observeForever(v -> { });
        vm.saved.observeForever(v -> savedEvent = v);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void defaults_matchPrototype() {
        vm.start(0, 0);
        assertEquals(LocalDate.of(2026, 10, 10), vm.plantedDate.getValue());
        assertEquals(CropEntity.MODE_GREENHOUSE, vm.plantMode.getValue());
        assertTrue(vm.remind.getValue());
        assertNull(vm.plotId.getValue());
        assertFalse(vm.editMode.getValue());
        assertFalse(vm.canSave.getValue());
    }

    @Test
    public void canSave_needsName() {
        vm.start(0, 0);
        vm.name.setValue("  ");
        assertFalse(vm.canSave.getValue());
        vm.name.setValue("草莓");
        assertTrue(vm.canSave.getValue());
    }

    @Test
    public void expectedDays_fromProfile() {
        vm.start(0, 0);
        vm.name.setValue("草莓");
        assertEquals(150, (int) vm.expectedDays.getValue());
        vm.name.setValue("");
        assertEquals(0, (int) vm.expectedDays.getValue());
    }

    @Test
    public void presetPlot_selectedAndModeFollowsPlot() {
        vm.start(0, 3);
        assertEquals(Long.valueOf(3), vm.plotId.getValue());
        assertEquals("东头露天地", vm.plotName.getValue());
        assertEquals(CropEntity.MODE_OPEN, vm.plantMode.getValue());
    }

    @Test
    public void selectPlot_nullMeansNone() {
        vm.start(0, 0);
        vm.selectPlot(2L);
        assertEquals("2号大棚", vm.plotName.getValue());
        vm.selectPlot(null);
        assertNull(vm.plotName.getValue());
    }

    @Test
    public void save_validatesArea() {
        vm.start(0, 0);
        vm.name.setValue("草莓");
        vm.save();
        assertEquals(R.string.add_crop_error_area, (int) vm.areaError.getValue());
        vm.area.setValue("0");
        assertEquals(0, (int) vm.areaError.getValue());   // 修改后清除
        vm.save();
        assertEquals(R.string.add_crop_error_area_positive, (int) vm.areaError.getValue());
        vm.area.setValue(".");
        vm.save();
        assertEquals(R.string.add_crop_error_area_format, (int) vm.areaError.getValue());
        assertTrue(repo.savedCrops.isEmpty());
    }

    @Test
    public void save_newCrop_buildsEntity() {
        vm.start(0, 0);
        vm.name.setValue(" 草莓 ");
        vm.variety.setValue("红颜");
        vm.selectPlot(2L);
        vm.area.setValue("0.3");
        vm.setPlantedDate(LocalDate.of(2026, 10, 10));
        vm.note.setValue("");
        vm.save();

        assertEquals(1, repo.savedCrops.size());
        CropEntity c = repo.savedCrops.get(0);
        assertEquals(0, c.id);
        assertEquals("草莓", c.name);
        assertEquals("红颜", c.variety);
        assertEquals(Long.valueOf(2), c.plotId);
        assertEquals(CropEntity.MODE_GREENHOUSE, c.plantMode);
        assertEquals(0.3, c.areaMu, 1e-9);
        assertEquals(LocalDate.of(2026, 10, 10).atStartOfDay(ZONE).toInstant().toEpochMilli(), c.plantedAt);
        assertEquals(150, c.expectedDays);
        assertTrue(c.remindEnabled);
        assertNull(c.note);
        assertEquals(CLOCK.millis(), c.createdAt);
        assertEquals(Boolean.TRUE, savedEvent);
    }

    @Test
    public void edit_prefillsAndKeepsStoredGrowthDays() {
        CropEntity c = new CropEntity();
        c.id = 7;
        c.name = "番茄";
        c.variety = "樱桃番茄";
        c.plotId = 1L;
        c.plantMode = CropEntity.MODE_POT;
        c.areaMu = 0.5;
        c.plantedAt = LocalDate.of(2026, 8, 3).atStartOfDay(ZONE).toInstant().toEpochMilli();
        c.expectedDays = 110;
        c.remindEnabled = false;
        c.coverPath = "file:///old.jpg";
        repo.crop = c;

        vm.start(7, 0);
        assertTrue(vm.editMode.getValue());
        assertEquals("番茄", vm.name.getValue());
        assertEquals("0.5", vm.area.getValue());
        assertEquals(LocalDate.of(2026, 8, 3), vm.plantedDate.getValue());
        assertEquals(CropEntity.MODE_POT, vm.plantMode.getValue());
        assertEquals("1号大棚", vm.plotName.getValue());
        assertFalse(vm.remind.getValue());
        assertEquals(110, (int) vm.expectedDays.getValue());

        vm.name.setValue("草莓");           // 改名后按生长期表重新计算
        assertEquals(150, (int) vm.expectedDays.getValue());
        vm.name.setValue("番茄");
        assertEquals(110, (int) vm.expectedDays.getValue());

        vm.removeCover();
        vm.save();
        assertEquals(7, repo.savedCrops.get(0).id);
        assertNull(repo.savedCrops.get(0).coverPath);
        assertEquals(Boolean.FALSE, savedEvent);
        assertEquals(Collections.singletonList("file:///old.jpg"), photos.deleted);
    }

    @Test
    public void edit_missingCrop_closesPage() {
        final Boolean[] closed = {null};
        vm.closePage.observeForever(v -> closed[0] = v);
        vm.start(99, 0);
        assertEquals(Boolean.TRUE, closed[0]);
        assertFalse(vm.canSave.getValue());
    }

    @Test
    public void cover_replacedAndUnsavedFilesAreDeleted() {
        vm.start(0, 0);
        vm.useCoverPhoto(null, false);
        assertEquals("file:///copied/crops/1.jpg", vm.coverPath.getValue());
        vm.useCoverPhoto(null, true);
        assertEquals("file:///copied/crops/2.jpg", vm.coverPath.getValue());
        assertEquals(Collections.singletonList("file:///copied/crops/1.jpg"), photos.deleted);
        assertEquals(1, photos.discarded.size());
        // 离开页面未保存：删除新复制的封面
        vm.onCleared();
        assertTrue(photos.deleted.contains("file:///copied/crops/2.jpg"));
    }

    @Test
    public void cover_keptAfterSave() {
        vm.start(0, 0);
        vm.name.setValue("草莓");
        vm.area.setValue("1");
        vm.useCoverPhoto(null, false);
        vm.save();
        assertEquals("file:///copied/crops/1.jpg", repo.savedCrops.get(0).coverPath);
        vm.onCleared();
        assertTrue(photos.deleted.isEmpty());
    }

    @Test
    public void save_errorShowsMessage() {
        repo.failSave = true;
        vm.start(0, 0);
        vm.name.setValue("草莓");
        vm.area.setValue("1");
        final String[] msg = {null};
        vm.toast.observeForever(m -> msg[0] = m);
        vm.save();
        assertEquals("请先登录", msg[0]);
        assertFalse(vm.saving.getValue());
        assertTrue(vm.canSave.getValue());
    }
}
