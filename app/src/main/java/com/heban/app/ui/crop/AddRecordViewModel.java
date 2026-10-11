package com.heban.app.ui.crop;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * C04 添加农事记录（技术准备文档 12.3）：表单字段随记录类型变化（见 {@link AddRecordFields}），
 * 「同步到收支记账」开启且费用大于 0 时由 Repository 在同一事务内写一笔支出；
 * 现场照片最多 9 张，复制到 filesDir/records，路径以 JSON 数组存入 photos。
 */
public class AddRecordViewModel extends BaseViewModel {
    public static final int MAX_PHOTOS = 9;

    /** 当前账号的作物（作物选择） */
    public final MutableLiveData<List<CropCard>> crops = new MutableLiveData<>();
    public final MutableLiveData<CropCard> crop = new MutableLiveData<>(null);
    public final MutableLiveData<FarmType> type = new MutableLiveData<>(FarmType.FERTILIZE);
    public final MediatorLiveData<AddRecordFields> fields = new MediatorLiveData<>();
    public final MutableLiveData<LocalDateTime> dateTime = new MutableLiveData<>();

    public final MutableLiveData<String> material = new MutableLiveData<>("");
    public final MutableLiveData<String> amount = new MutableLiveData<>("");
    public final MutableLiveData<String> unit = new MutableLiveData<>("");
    public final MutableLiveData<String> cost = new MutableLiveData<>("");
    public final MutableLiveData<String> note = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> sync = new MutableLiveData<>(true);
    /** 费用（分），输入不合法时为 AddRecordMoney.INVALID */
    public final MediatorLiveData<Long> costCents = new MediatorLiveData<>();
    /** 现场照片（file:// 路径） */
    public final MutableLiveData<List<String>> photos = new MutableLiveData<>(Collections.emptyList());

    /** 输入框下方的错误（字符串资源，0 表示无；required / number / positive 需要拼上字段名） */
    public final MutableLiveData<Integer> materialError = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> amountError = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> costError = new MutableLiveData<>(0);

    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> photoBusy = new MutableLiveData<>(false);
    public final MediatorLiveData<Boolean> canSave = new MediatorLiveData<>();

    public final SingleLiveEvent<Integer> messageRes = new SingleLiveEvent<>();
    /** 照片超出上限的提示（参数为上限） */
    public final SingleLiveEvent<Integer> photoLimit = new SingleLiveEvent<>();
    /** 保存成功：true 表示同时写入了一笔支出 */
    public final SingleLiveEvent<Boolean> saved = new SingleLiveEvent<>();

    private final CropRepository repo;
    private final PhotoPickerStore photoStore;
    private final Gson gson;
    private final Clock clock;
    /** 切换记录类型时保存各类型已填写的物料 / 用量 / 单位 */
    private final Map<FarmType, String[]> drafts = new EnumMap<>(FarmType.class);

    private final Observer<String> materialWatcher = v -> clear(materialError);
    private final Observer<String> amountWatcher = v -> clear(amountError);
    private final Observer<String> costWatcher = v -> clear(costError);

    private boolean started;
    private boolean committed;
    private long presetCropId;

    public AddRecordViewModel(AppContainer c) {
        this(c, c.cropRepository, new PhotoPickerFileStore(c.app), c.gson, Clock.systemDefaultZone());
    }

    @VisibleForTesting
    AddRecordViewModel(AppContainer c, CropRepository repo, PhotoPickerStore photoStore, Gson gson, Clock clock) {
        super(c);
        this.repo = repo;
        this.photoStore = photoStore;
        this.gson = gson;
        this.clock = clock;
        dateTime.setValue(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES));
        unit.setValue(AddRecordFields.of(FarmType.FERTILIZE).defaultUnit());

        fields.addSource(type, t -> fields.setValue(AddRecordFields.of(t == null ? FarmType.OTHER : t)));
        costCents.setValue(0L);
        costCents.addSource(cost, v -> costCents.setValue(AddRecordMoney.parseCents(v)));

        canSave.setValue(false);
        canSave.addSource(crop, v -> updateCanSave());
        canSave.addSource(saving, v -> updateCanSave());
        canSave.addSource(photoBusy, v -> updateCanSave());

        material.observeForever(materialWatcher);
        amount.observeForever(amountWatcher);
        cost.observeForever(costWatcher);
    }

    /** 页面参数：cropId > 0 时预选作物；farmType 为 FarmType 名称时预选记录类型（只在第一次调用时生效） */
    public void start(long cropId, @Nullable String farmType) {
        if (started) return;
        started = true;
        presetCropId = cropId;
        if (farmType != null && !farmType.isEmpty()) selectType(FarmType.of(farmType));
        add(repo.observeCropCards(null)
                .onErrorReturnItem(Collections.emptyList())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCrops, e -> { }));
    }

    /** 打开页面时是否指定了作物（从作物详情进入，作物不可更换） */
    public boolean isCropPreset() {
        return presetCropId > 0;
    }

    private void onCrops(List<CropCard> list) {
        List<CropCard> cards = list == null ? Collections.emptyList() : list;
        crops.setValue(cards);
        CropCard current = crop.getValue();
        long wanted = current != null ? current.id : presetCropId;
        CropCard match = null;
        for (CropCard c : cards) {
            if (c.id == wanted) match = c;
        }
        if (match == null && current == null && presetCropId <= 0 && cards.size() == 1) match = cards.get(0);
        if (match != null || current != null) crop.setValue(match);
    }

    public void selectCrop(@Nullable CropCard card) {
        crop.setValue(card);
    }

    public void selectType(@NonNull FarmType t) {
        FarmType old = type.getValue();
        if (old == t) return;
        if (old != null) drafts.put(old, new String[]{material.getValue(), amount.getValue(), unit.getValue()});
        String[] d = drafts.get(t);
        AddRecordFields f = AddRecordFields.of(t);
        type.setValue(t);
        material.setValue(d == null ? "" : d[0]);
        amount.setValue(d == null ? "" : d[1]);
        unit.setValue(d == null || d[2] == null ? f.defaultUnit() : d[2]);
        materialError.setValue(0);
        amountError.setValue(0);
    }

    public void setDateTime(@NonNull LocalDateTime value) {
        dateTime.setValue(value.truncatedTo(ChronoUnit.MINUTES));
    }

    // ---------- 现场照片 ----------

    public int remainingPhotos() {
        List<String> list = photos.getValue();
        return MAX_PHOTOS - (list == null ? 0 : list.size());
    }

    /** 照片选择器返回的图片：超出 9 张的部分丢弃并提示，其余复制到 filesDir/records */
    public void addPhotos(@Nullable List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        int remaining = remainingPhotos();
        List<Uri> accepted = new ArrayList<>(uris.subList(0, Math.min(uris.size(), Math.max(remaining, 0))));
        if (accepted.size() < uris.size()) photoLimit.setValue(MAX_PHOTOS);
        if (accepted.isEmpty()) return;
        photoBusy.setValue(true);
        add(photoStore.copyAll(accepted, PhotoPickerStore.DIR_RECORDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(paths -> {
                    photoBusy.setValue(false);
                    List<String> next = new ArrayList<>(photos.getValue() == null
                            ? Collections.emptyList() : photos.getValue());
                    for (String p : paths) {
                        if (next.size() < MAX_PHOTOS) next.add(p);
                        else photoStore.delete(Collections.singletonList(p));
                    }
                    photos.setValue(next);
                }, e -> {
                    photoBusy.setValue(false);
                    messageRes.setValue(R.string.add_crop_photo_failed);
                }));
    }

    public void removePhoto(@NonNull String path) {
        List<String> next = new ArrayList<>(photos.getValue() == null ? Collections.emptyList() : photos.getValue());
        if (next.remove(path)) {
            photos.setValue(next);
            photoStore.delete(Collections.singletonList(path));
        }
    }

    // ---------- 保存 ----------

    public void save() {
        if (!Boolean.TRUE.equals(canSave.getValue())) return;
        CropCard selected = crop.getValue();
        if (selected == null) {
            messageRes.setValue(R.string.add_record_error_crop);
            return;
        }
        FarmType t = type.getValue() == null ? FarmType.OTHER : type.getValue();
        AddRecordFields f = AddRecordFields.of(t);
        boolean ok = true;

        String mat = trim(material.getValue());
        if (f.materialRequired() && mat.isEmpty()) {
            materialError.setValue(R.string.add_record_error_required);
            ok = false;
        }
        double amountValue = 0;
        if (f.hasAmount()) {
            Double parsed = AddCropHarvest.parseDecimal(amount.getValue());
            if (parsed == null) {
                if (f.amountRequired) {
                    amountError.setValue(R.string.add_record_error_required);
                    ok = false;
                }
            } else if (parsed.isNaN()) {
                amountError.setValue(R.string.add_record_error_number);
                ok = false;
            } else if (parsed <= 0) {
                amountError.setValue(R.string.add_record_error_positive);
                ok = false;
            } else {
                amountValue = parsed;
            }
        }
        long cents = AddRecordMoney.parseCents(cost.getValue());
        if (cents == AddRecordMoney.INVALID) {
            costError.setValue(AddRecordMoney.hasTooManyDecimals(cost.getValue())
                    ? R.string.add_record_error_cost_decimals : R.string.add_record_error_cost);
            ok = false;
        }
        if (!ok) return;

        FarmRecordEntity r = new FarmRecordEntity();
        r.cropId = selected.id;
        r.type = t.name();
        LocalDateTime when = dateTime.getValue() == null ? LocalDateTime.now(clock) : dateTime.getValue();
        r.date = when.atZone(clock.getZone()).toInstant().toEpochMilli();
        r.material = f.hasMaterial() && !mat.isEmpty() ? mat : null;
        r.amount = amountValue;
        r.unit = f.hasAmount() && amountValue > 0 ? unit.getValue() : null;
        r.costCent = cents;
        List<String> list = photos.getValue();
        r.photos = list == null || list.isEmpty() ? null : gson.toJson(list);
        String n = trim(note.getValue());
        r.note = n.isEmpty() ? null : n;
        r.createdAt = clock.millis();

        boolean syncLedger = Boolean.TRUE.equals(sync.getValue());
        saving.setValue(true);
        add(repo.saveRecord(r, syncLedger)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id -> {
                    saving.setValue(false);
                    committed = true;
                    saved.setValue(syncLedger && cents > 0);
                }, e -> {
                    saving.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    @Override
    protected void onCleared() {
        material.removeObserver(materialWatcher);
        amount.removeObserver(amountWatcher);
        cost.removeObserver(costWatcher);
        super.onCleared();
        // 没有保存就离开：删除已复制的照片
        List<String> list = photos.getValue();
        if (!committed && list != null && !list.isEmpty()) photoStore.delete(list);
    }

    private void updateCanSave() {
        canSave.setValue(crop.getValue() != null && !Boolean.TRUE.equals(saving.getValue())
                && !Boolean.TRUE.equals(photoBusy.getValue()));
    }

    private static void clear(MutableLiveData<Integer> error) {
        Integer e = error.getValue();
        if (e != null && e != 0) error.setValue(0);
    }

    private static String trim(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}
