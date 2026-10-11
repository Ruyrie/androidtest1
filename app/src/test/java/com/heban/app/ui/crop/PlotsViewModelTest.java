package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.remote.BusinessException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlotsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CropsFakeRepository repo;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new CropsFakeRepository();
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private static PlotEntity plot(long id, String name) {
        PlotEntity p = new PlotEntity();
        p.id = id;
        p.name = name;
        p.areaMu = 0.5;
        return p;
    }

    @Test
    public void emptyAndLoaded() {
        PlotsViewModel vm = new PlotsViewModel(null, repo);
        assertTrue(vm.loaded.getValue());
        assertFalse(vm.hasPlots.getValue());
        PlotCard c = new PlotCard();
        c.plot = plot(1, "1号大棚");
        repo.plotCards.onNext(Collections.singletonList(c));
        assertTrue(vm.hasPlots.getValue());
    }

    @Test
    public void readingValidation() {
        PlotsViewModel vm = new PlotsViewModel(null, repo);
        assertEquals(R.string.plots_error_temp, vm.recordReading(1, "", "70"));
        assertEquals(R.string.plots_error_temp, vm.recordReading(1, "61", "70"));
        assertEquals(R.string.plots_error_humidity, vm.recordReading(1, "22", "101"));
        assertTrue(repo.readings.isEmpty());
        assertEquals(0, vm.recordReading(1, "-2.5", "89"));
        assertEquals(1, repo.readings.size());
        assertEquals(-2.5, repo.readings.get(0)[1], 0.001);
        assertEquals(89, repo.readings.get(0)[2], 0.001);
    }

    @Test
    public void deletePlot() {
        PlotsViewModel vm = new PlotsViewModel(null, repo);
        vm.deletePlot(3);
        assertEquals(3L, (long) repo.deletedPlots.get(0));
    }

    @Test
    public void editor_validatesAndCreates() {
        PlotEditViewModel vm = new PlotEditViewModel(null, repo);
        int[] saved = {0};
        vm.saved.observeForever(s -> saved[0]++);
        vm.start(0);
        vm.save();
        assertEquals(R.string.plots_error_name, (int) vm.nameError.getValue());
        assertEquals(R.string.plots_error_area, (int) vm.areaError.getValue());
        assertTrue(repo.savedPlots.isEmpty());

        vm.name.setValue(" 东地块 ");
        vm.area.setValue("3");
        vm.selectType(PlotEntity.TYPE_OPEN);
        vm.soil.setValue("沙壤土");
        vm.irrigation.setValue("漫灌");
        vm.save();
        assertEquals(0, (int) vm.nameError.getValue());
        assertEquals(1, saved[0]);
        PlotEntity p = repo.savedPlots.get(0);
        assertEquals(0, p.id);
        assertEquals("东地块", p.name);
        assertEquals(3.0, p.areaMu, 0.0001);
        assertEquals(PlotEntity.TYPE_OPEN, p.type);
        assertEquals("沙壤土", p.soil);
        assertNull(p.structure);
    }

    @Test
    public void editor_loadsExistingPlotAndKeepsId() {
        PlotEntity existing = plot(5, "1号大棚");
        existing.structure = "钢架薄膜棚";
        existing.indoorTemp = 22.0;
        repo.plots.onNext(Collections.singletonList(existing));
        PlotEditViewModel vm = new PlotEditViewModel(null, repo);
        vm.start(5);
        assertTrue(vm.isEdit.getValue());
        assertEquals("1号大棚", vm.name.getValue());
        assertEquals("0.5", vm.area.getValue());
        assertEquals("钢架薄膜棚", vm.structure.getValue());
        vm.area.setValue("0.7");
        vm.save();
        PlotEntity p = repo.savedPlots.get(0);
        assertEquals(5, p.id);
        assertEquals(0.7, p.areaMu, 0.0001);
        assertEquals(22.0, p.indoorTemp, 0.0001);
    }

    @Test
    public void editor_showsRepositoryError() {
        repo.writeError = new BusinessException("NOT_LOGGED_IN", "请先登录");
        PlotEditViewModel vm = new PlotEditViewModel(null, repo);
        vm.toast.observeForever(t -> { });
        vm.start(0);
        vm.name.setValue("1号大棚");
        vm.area.setValue("0.5");
        vm.save();
        assertEquals("请先登录", vm.toast.getValue());
        assertFalse(vm.saving.getValue());
    }
}
