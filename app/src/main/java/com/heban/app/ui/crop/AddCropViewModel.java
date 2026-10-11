package com.heban.app.ui.crop;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.heban.app.R;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropProfile;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * C03 添加 / 编辑作物（新增与编辑共用，技术准备文档 12.3）。
 * 「保存」在作物名称不为空时可点；点击时校验名称与面积（大于 0），错误显示在对应输入框下方。
 * 预计采收 = 定植日期 + 作物默认生长期（作物生长期表）；编辑时作物名称未改动则沿用已保存的生长期。
 */
public class AddCropViewModel extends BaseViewModel {
    public final MutableLiveData<String> name = new MutableLiveData<>("");
    public final MutableLiveData<String> variety = new MutableLiveData<>("");
    public final MutableLiveData<String> area = new MutableLiveData<>("");
    public final MutableLiveData<String> note = new MutableLiveData<>("");
    /** 种植方式：CropEntity.MODE_* */
    public final MutableLiveData<String> plantMode = new MutableLiveData<>(CropEntity.MODE_GREENHOUSE);
    public final MutableLiveData<LocalDate> plantedDate = new MutableLiveData<>();
    public final MutableLiveData<Boolean> remind = new MutableLiveData<>(true);
    /** 封面照片（file:// 路径），null 表示未选择 */
    public final MutableLiveData<String> coverPath = new MutableLiveData<>(null);
    /** 所选地块 id，null 表示「不选择地块」 */
    public final MutableLiveData<Long> plotId = new MutableLiveData<>(null);
    public final MutableLiveData<List<PlotEntity>> plots = new MutableLiveData<>(Collections.emptyList());
    /** 作物名称输入建议（生长期表，可能为空） */
    public final MutableLiveData<List<CropProfile>> profiles = new MutableLiveData<>(Collections.emptyList());

    /** 当前作物名称对应的生长期（天），名称为空时为 0 */
    public final MediatorLiveData<Integer> expectedDays = new MediatorLiveData<>();
    /** 所选地块名称，null 表示「不选择地块」 */
    public final MediatorLiveData<String> plotName = new MediatorLiveData<>();

    /** 输入框下方的错误（字符串资源，0 表示无） */
    public final MutableLiveData<Integer> nameError = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> areaError = new MutableLiveData<>(0);

    public final MutableLiveData<Boolean> editMode = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> photoBusy = new MutableLiveData<>(false);
    public final MediatorLiveData<Boolean> canSave = new MediatorLiveData<>();

    /** 一次性提示（字符串资源） */
    public final SingleLiveEvent<Integer> messageRes = new SingleLiveEvent<>();
    /** 保存成功：true 为新增，false 为编辑 */
    public final SingleLiveEvent<Boolean> saved = new SingleLiveEvent<>();
    /** 编辑的作物不存在：提示后关闭页面 */
    public final SingleLiveEvent<Boolean> closePage = new SingleLiveEvent<>();

    private final CropRepository repo;
    private final PhotoPickerStore photos;
    private final Clock clock;
    private final Map<String, Integer> profileDays = new HashMap<>();
    /** 本次打开页面后复制进来的封面文件；未保存或被替换时删除 */
    private final Set<String> newCovers = new HashSet<>();

    private final Observer<String> nameWatcher = v -> {
        if (!isBlank(v) && hasError(nameError)) nameError.setValue(0);
    };
    private final Observer<String> areaWatcher = v -> {
        if (hasError(areaError)) areaError.setValue(0);
    };

    private boolean started;
    private boolean loaded = true;
    @Nullable private CropEntity editing;
    /** 编辑前的封面：保存时被更换或移除则删除原文件 */
    @Nullable private String editingCover;
    @Nullable private Long pendingPlotId;
    private boolean plotTouched;

    public AddCropViewModel(AppContainer c) {
        this(c, c.cropRepository, new PhotoPickerFileStore(c.app), Clock.systemDefaultZone());
    }

    @VisibleForTesting
    AddCropViewModel(AppContainer c, CropRepository repo, PhotoPickerStore photos, Clock clock) {
        super(c);
        this.repo = repo;
        this.photos = photos;
        this.clock = clock;
        plantedDate.setValue(LocalDate.now(clock));

        expectedDays.setValue(0);
        expectedDays.addSource(name, n -> expectedDays.setValue(expectedDaysFor(n)));

        plotName.addSource(plots, p -> plotName.setValue(findPlotName()));
        plotName.addSource(plotId, p -> plotName.setValue(findPlotName()));

        canSave.setValue(false);
        canSave.addSource(name, v -> updateCanSave());
        canSave.addSource(saving, v -> updateCanSave());
        canSave.addSource(photoBusy, v -> updateCanSave());

        // 修改后清除对应的错误
        name.observeForever(nameWatcher);
        area.observeForever(areaWatcher);
    }

    /** 页面参数：cropId > 0 为编辑；plotId > 0 时预选地块（只在第一次调用时生效） */
    public void start(long cropId, long presetPlotId) {
        if (started) return;
        started = true;
        if (presetPlotId > 0) pendingPlotId = presetPlotId;
        add(repo.observePlots()
                .onErrorReturnItem(Collections.emptyList())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onPlots, e -> { }));
        add(Single.fromCallable(repo::allProfiles)
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(Collections.emptyList())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> profiles.setValue(list == null ? Collections.emptyList() : list), e -> { }));
        if (cropId > 0) {
            editMode.setValue(true);
            loaded = false;
            updateCanSave();
            add(repo.getCrop(cropId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::fill, e -> {
                        messageRes.setValue(R.string.add_crop_not_found);
                        closePage.setValue(true);
                    }));
        }
    }

    private void fill(CropEntity crop) {
        editing = crop;
        editingCover = crop.coverPath;
        loaded = true;
        name.setValue(crop.name);
        variety.setValue(crop.variety == null ? "" : crop.variety);
        area.setValue(AddCropHarvest.formatDecimal(crop.areaMu));
        note.setValue(crop.note == null ? "" : crop.note);
        plantMode.setValue(crop.plantMode);
        if (crop.plantedAt > 0) plantedDate.setValue(AddCropHarvest.toLocalDate(crop.plantedAt, zone()));
        remind.setValue(crop.remindEnabled);
        coverPath.setValue(crop.coverPath);
        pendingPlotId = null;
        plotId.setValue(crop.plotId);
        expectedDays.setValue(expectedDaysFor(crop.name));
        areaError.setValue(0);
        updateCanSave();
    }

    private void onPlots(List<PlotEntity> list) {
        plots.setValue(list == null ? Collections.emptyList() : list);
        if (pendingPlotId != null && !plotTouched) {
            PlotEntity p = findPlot(pendingPlotId);
            if (p != null) {
                plotId.setValue(p.id);
                applyPlotType(p);
                pendingPlotId = null;
            }
        }
    }

    /** 下拉选择地块；null 为「不选择地块」。新增作物时种植方式跟随地块类型（大棚 / 露天） */
    public void selectPlot(@Nullable Long id) {
        plotTouched = true;
        pendingPlotId = null;
        plotId.setValue(id);
        if (id != null && editing == null) {
            PlotEntity p = findPlot(id);
            if (p != null) applyPlotType(p);
        }
    }

    private void applyPlotType(PlotEntity p) {
        if (PlotEntity.TYPE_GREENHOUSE.equals(p.type)) plantMode.setValue(CropEntity.MODE_GREENHOUSE);
        else if (PlotEntity.TYPE_OPEN.equals(p.type)) plantMode.setValue(CropEntity.MODE_OPEN);
    }

    public void selectMode(@NonNull String mode) {
        plantMode.setValue(mode);
    }

    public void setPlantedDate(@NonNull LocalDate date) {
        plantedDate.setValue(date);
    }

    // ---------- 封面照片 ----------

    /** 照片选择器或相机返回的图片：复制到 filesDir/crops 后作为封面；fromCamera 时复制后删除相机临时文件 */
    public void useCoverPhoto(@NonNull Uri source, boolean fromCamera) {
        photoBusy.setValue(true);
        Single<String> copy = photos.copy(source, PhotoPickerStore.DIR_CROPS);
        if (fromCamera) copy = copy.doFinally(() -> photos.discardCameraTarget(source));
        add(copy.observeOn(AndroidSchedulers.mainThread())
                .subscribe(path -> {
                    photoBusy.setValue(false);
                    newCovers.add(path);
                    replaceCover(path);
                }, e -> {
                    photoBusy.setValue(false);
                    messageRes.setValue(R.string.add_crop_photo_failed);
                }));
    }

    public void removeCover() {
        replaceCover(null);
    }

    public boolean hasCover() {
        return !isBlank(coverPath.getValue());
    }

    private void replaceCover(@Nullable String path) {
        String old = coverPath.getValue();
        if (old != null && !old.equals(path) && newCovers.remove(old)) {
            photos.delete(Collections.singletonList(old));
        }
        coverPath.setValue(path);
    }

    // ---------- 保存 ----------

    public void save() {
        if (!Boolean.TRUE.equals(canSave.getValue())) return;
        String n = trim(name.getValue());
        boolean ok = true;
        if (n.isEmpty()) {
            nameError.setValue(R.string.add_crop_error_name);
            ok = false;
        }
        Double areaMu = AddCropHarvest.parseDecimal(area.getValue());
        if (areaMu == null) {
            areaError.setValue(R.string.add_crop_error_area);
            ok = false;
        } else if (areaMu.isNaN()) {
            areaError.setValue(R.string.add_crop_error_area_format);
            ok = false;
        } else if (areaMu <= 0) {
            areaError.setValue(R.string.add_crop_error_area_positive);
            ok = false;
        }
        if (!ok) return;

        boolean isNew = editing == null;
        CropEntity crop = isNew ? new CropEntity() : editing;
        crop.name = n;
        crop.variety = emptyToNull(variety.getValue());
        crop.plotId = plotId.getValue();
        crop.plantMode = plantMode.getValue() == null ? CropEntity.MODE_GREENHOUSE : plantMode.getValue();
        crop.areaMu = areaMu;
        LocalDate planted = plantedDate.getValue() == null ? LocalDate.now(clock) : plantedDate.getValue();
        crop.plantedAt = AddCropHarvest.startOfDayMillis(planted, zone());
        Integer days = expectedDays.getValue();
        crop.expectedDays = days == null ? 0 : days;
        crop.coverPath = coverPath.getValue();
        crop.remindEnabled = Boolean.TRUE.equals(remind.getValue());
        crop.note = emptyToNull(note.getValue());
        if (isNew) crop.createdAt = clock.millis();

        String oldCover = isNew ? null : editingCover;
        saving.setValue(true);
        add(repo.saveCrop(crop)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id -> {
                    saving.setValue(false);
                    newCovers.remove(crop.coverPath);
                    // 编辑时更换或移除了封面：删除原来复制进来的文件
                    if (oldCover != null && !oldCover.equals(crop.coverPath)) {
                        photos.delete(Collections.singletonList(oldCover));
                    }
                    saved.setValue(isNew);
                }, e -> {
                    saving.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    @Override
    protected void onCleared() {
        name.removeObserver(nameWatcher);
        area.removeObserver(areaWatcher);
        super.onCleared();
        // 未保存（或保存后被替换掉）的新封面文件
        if (!newCovers.isEmpty()) photos.delete(new ArrayList<>(newCovers));
        newCovers.clear();
    }

    // ---------- 内部 ----------

    private void updateCanSave() {
        canSave.setValue(loaded && !isBlank(name.getValue())
                && !Boolean.TRUE.equals(saving.getValue()) && !Boolean.TRUE.equals(photoBusy.getValue()));
    }

    @VisibleForTesting
    int expectedDaysFor(@Nullable String rawName) {
        String n = trim(rawName);
        if (n.isEmpty()) return 0;
        if (editing != null && n.equals(editing.name) && editing.expectedDays > 0) return editing.expectedDays;
        Integer cached = profileDays.get(n);
        if (cached != null) return cached;
        int days = 0;
        try {
            CropProfile p = repo.profileFor(n);
            if (p != null && p.expectedDays > 0) days = p.expectedDays;
        } catch (RuntimeException ignored) {
            // 生长期表读取失败时不显示预计采收
        }
        profileDays.put(n, days);
        return days;
    }

    @Nullable
    private String findPlotName() {
        Long id = plotId.getValue();
        if (id == null) return null;
        PlotEntity p = findPlot(id);
        return p == null ? null : p.name;
    }

    @Nullable
    private PlotEntity findPlot(long id) {
        List<PlotEntity> list = plots.getValue();
        if (list == null) return null;
        for (PlotEntity p : list) {
            if (p.id == id) return p;
        }
        return null;
    }

    private ZoneId zone() {
        return clock.getZone();
    }

    private static boolean hasError(MutableLiveData<Integer> error) {
        Integer e = error.getValue();
        return e != null && e != 0;
    }

    private static boolean isBlank(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    @Nullable
    private static String emptyToNull(@Nullable String s) {
        String t = trim(s);
        return t.isEmpty() ? null : t;
    }
}
