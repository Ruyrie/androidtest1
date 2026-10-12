package com.heban.app.ui.discover.scan;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.heban.app.data.local.entity.ScanRecordEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.model.ScanOutcome;
import com.heban.app.data.model.ScanResult;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.PestRepository;
import com.heban.app.data.repository.VisionRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * D02 拍照识别与 D03 识别结果共用的 ViewModel（作用域为 scan_graph）：
 * 识别模式、闪光灯、相机权限状态、识别记录；拍照 / 选图后立即进入 D03 并显示「正在识别…」，
 * 识别成功、照片不可用、失败三种结果；重新识别、打开历史记录、保存到作物记录。
 */
public class ScanViewModel extends BaseViewModel {
    /** D03 的状态：空闲（还没有识别）/ 识别中 / 成功 / 照片不可用 / 失败 */
    public enum Phase { IDLE, LOADING, SUCCESS, IMAGE_BAD, ERROR }

    /** D02「历史」底部弹窗的一行 */
    public static final class HistoryItem {
        public final long id;
        public final String imagePath;
        @Nullable public final ScanResult result;
        public final ScanMode mode;
        public final long createdAt;
        public final boolean saved;

        HistoryItem(ScanRecordEntity e, @Nullable ScanResult result) {
            id = e.id;
            imagePath = e.imagePath;
            this.result = result;
            mode = parseMode(e.mode);
            createdAt = e.createdAt;
            saved = e.cropId != null;
        }
    }

    private final VisionRepository vision;
    private final CropRepository cropRepo;
    private final PestRepository pestRepo;
    private final Gson gson;

    // —— D02 ——
    public final MutableLiveData<ScanMode> mode = new MutableLiveData<>(ScanMode.PEST);
    public final MutableLiveData<Boolean> flashOn = new MutableLiveData<>(false);
    /** 相机权限被拒绝：显示页内授权说明，相册仍可用 */
    public final MutableLiveData<Boolean> cameraDenied = new MutableLiveData<>(false);
    private boolean permissionRequested;
    private final MutableLiveData<List<HistoryItem>> history = new MutableLiveData<>();
    private boolean historyStarted;
    /** 拍照 / 选图或打开历史记录后进入 D03；值为记录 id（新识别为 0） */
    public final SingleLiveEvent<Long> openResult = new SingleLiveEvent<>();

    // —— D03 ——
    public final MutableLiveData<Phase> phase = new MutableLiveData<>(Phase.IDLE);
    /** 刚拍摄 / 选择的照片（识别期间立即显示） */
    public final MutableLiveData<Bitmap> photo = new MutableLiveData<>();
    public final MutableLiveData<ScanOutcome> outcome = new MutableLiveData<>();
    /** 失败时的提示（AppError.message） */
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    /** 「其他可能」中按名称匹配到的本地百科 id */
    public final MutableLiveData<Map<String, Long>> otherPestIds = new MutableLiveData<>(Collections.emptyMap());
    public final MutableLiveData<List<CropCard>> crops = new MutableLiveData<>(Collections.emptyList());
    /** 与识别出的作物同名的作物（第一个为默认保存目标） */
    public final MediatorLiveData<List<CropCard>> matchedCrops = new MediatorLiveData<>();
    public final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    /** 保存成功：作物标签（「黄瓜 · 2号大棚」），用于 Snackbar */
    public final SingleLiveEvent<String> savedTo = new SingleLiveEvent<>();

    @Nullable private Bitmap lastPhoto;
    private long openedRecordId;
    @Nullable private Disposable current;
    @Nullable private Disposable otherLookup;

    public ScanViewModel(AppContainer c) {
        this(c, c.visionRepository, c.cropRepository, c.pestRepository, c.gson);
    }

    ScanViewModel(@Nullable AppContainer c, @NonNull VisionRepository vision, @NonNull CropRepository cropRepo,
                  @NonNull PestRepository pestRepo, @NonNull Gson gson) {
        super(c);
        this.vision = vision;
        this.cropRepo = cropRepo;
        this.pestRepo = pestRepo;
        this.gson = gson;
        matchedCrops.setValue(Collections.emptyList());
        matchedCrops.addSource(crops, x -> updateMatches());
        matchedCrops.addSource(outcome, x -> updateMatches());
        add(cropRepo.observeCropCards(null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> crops.setValue(list == null ? Collections.emptyList() : list),
                        e -> crops.setValue(Collections.emptyList())));
    }

    // ———————————————— D02 ————————————————

    public void selectMode(@NonNull ScanMode m) {
        if (m != mode.getValue()) mode.setValue(m);
    }

    public void toggleFlash() {
        flashOn.setValue(!Boolean.TRUE.equals(flashOn.getValue()));
    }

    /** 进入拍照页时只主动申请一次相机权限（同一次拍照流程内） */
    public boolean shouldRequestPermission() {
        if (permissionRequested) return false;
        permissionRequested = true;
        return true;
    }

    public void setCameraDenied(boolean denied) {
        if (!Boolean.valueOf(denied).equals(cameraDenied.getValue())) cameraDenied.setValue(denied);
    }

    /** 识别记录（打开「历史」时开始订阅） */
    public LiveData<List<HistoryItem>> history() {
        if (!historyStarted) {
            historyStarted = true;
            add(vision.observeHistory()
                    .map(this::toHistory)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(history::setValue, e -> history.setValue(Collections.emptyList())));
        }
        return history;
    }

    public void deleteRecord(long recordId) {
        add(vision.deleteRecord(recordId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> { }, e -> toast.setValue(AppError.from(e).message)));
    }

    /** 拍照得到的照片（已转正） */
    public void recognize(@NonNull Bitmap upright) {
        recognizeFrom(() -> upright);
    }

    /** 相册选图：解码在后台进行，先进入 D03 显示识别中 */
    public void recognizeFrom(@NonNull Callable<Bitmap> decoder) {
        resetResult();
        phase.setValue(Phase.LOADING);
        openResult.setValue(0L);
        ScanMode m = currentMode();
        String hint = ScanFormat.cropHint(crops.getValue());
        current = Single.fromCallable(decoder)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(bmp -> {
                    lastPhoto = bmp;
                    photo.setValue(bmp);
                    return vision.recognize(bmp, m, hint).observeOn(AndroidSchedulers.mainThread());
                })
                .subscribe(this::showOutcome, this::showError);
        add(current);
    }

    /** D02「历史」中点一条记录：载入保存的结果并进入 D03 */
    public void openHistory(long recordId) {
        openedRecordId = 0;
        openRecord(recordId);
        openResult.setValue(recordId);
    }

    // ———————————————— D03 ————————————————

    /** D03 带 NavArgs.SCAN_RECORD_ID 打开：显示保存的结果（同一条记录不重复载入） */
    public void openRecord(long recordId) {
        if (recordId <= 0 || recordId == openedRecordId) return;
        resetResult();
        openedRecordId = recordId;
        phase.setValue(Phase.LOADING);
        current = vision.getOutcome(recordId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showOutcome, this::showError);
        add(current);
    }

    /** 顶栏「刷新」/ 失败时「重试」：有记录时用同一张照片重新识别，否则重新提交刚才的照片 */
    public void retry() {
        if (phase.getValue() == Phase.LOADING) return;
        ScanOutcome o = outcome.getValue();
        Single<ScanOutcome> source;
        if (o != null && o.recordId > 0) {
            source = vision.retry(o.recordId);
        } else if (lastPhoto != null) {
            source = vision.recognize(lastPhoto, currentMode(), ScanFormat.cropHint(crops.getValue()));
        } else if (openedRecordId > 0) {
            source = vision.getOutcome(openedRecordId);
        } else {
            return;
        }
        dispose(current);
        dispose(otherLookup);
        otherPestIds.setValue(Collections.emptyMap());
        phase.setValue(Phase.LOADING);
        errorMessage.setValue(null);
        current = source.observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showOutcome, this::showError);
        add(current);
    }

    /** 保存到某个作物的农事记录 */
    public void saveToCrop(@NonNull CropCard crop) {
        ScanOutcome o = outcome.getValue();
        if (o == null || o.recordId <= 0 || Boolean.TRUE.equals(saved.getValue())
                || Boolean.TRUE.equals(saving.getValue())) return;
        saving.setValue(true);
        String label = ScanFormat.cropLabel(crop);
        add(vision.saveToCrop(o.recordId, crop.id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    saving.setValue(false);
                    o.cropId = crop.id;
                    saved.setValue(true);
                    savedTo.setValue(label);
                }, e -> {
                    saving.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    /** 默认保存目标：与识别出的作物同名的第一个作物，没有时为 null（由用户选择） */
    @Nullable
    public CropCard saveTarget() {
        List<CropCard> m = matchingCrops();
        return m.isEmpty() ? null : m.get(0);
    }

    /** 与识别出的作物（result.crop）同名的作物 */
    @NonNull
    public List<CropCard> matchingCrops() {
        ScanOutcome o = outcome.getValue();
        String crop = o == null || o.result == null ? null : o.result.crop;
        return ScanFormat.matchingCrops(crops.getValue(), crop);
    }

    /** 「问 AI」带入的识别结论 */
    @NonNull
    public String aiPrompt() {
        ScanOutcome o = outcome.getValue();
        return ScanFormat.aiPrompt(o == null ? null : o.result);
    }

    @NonNull
    public String publishText() {
        ScanOutcome o = outcome.getValue();
        return ScanFormat.publishText(o == null ? null : o.result);
    }

    @Nullable
    public Long pestId() {
        ScanOutcome o = outcome.getValue();
        return o == null ? null : o.pestId;
    }

    /** 「其他可能」中某一项匹配到的百科 id，未匹配为 null */
    @Nullable
    public Long otherPestId(@Nullable String name) {
        Map<String, Long> m = otherPestIds.getValue();
        return name == null || m == null ? null : m.get(name);
    }

    public boolean hasResult() {
        return phase.getValue() != Phase.IDLE;
    }

    // ———————————————— 内部 ————————————————

    private ScanMode currentMode() {
        ScanMode m = mode.getValue();
        return m == null ? ScanMode.PEST : m;
    }

    private void resetResult() {
        dispose(current);
        dispose(otherLookup);
        current = null;
        lastPhoto = null;
        openedRecordId = 0;
        photo.setValue(null);
        outcome.setValue(null);
        errorMessage.setValue(null);
        otherPestIds.setValue(Collections.emptyMap());
        saved.setValue(false);
        saving.setValue(false);
    }

    private void showOutcome(@NonNull ScanOutcome o) {
        ScanResult r = o.result;
        if (r != null && !r.imageOk) {
            outcome.setValue(o);
            saved.setValue(o.cropId != null);
            phase.setValue(Phase.IMAGE_BAD);
            return;
        }
        if (r == null || r.top == null) {
            showError(new JsonParseException("missing top"));
            return;
        }
        outcome.setValue(o);
        saved.setValue(o.cropId != null);
        phase.setValue(Phase.SUCCESS);
        lookUpOthers(r);
    }

    private void showError(@NonNull Throwable e) {
        errorMessage.setValue(AppError.from(e).message);
        phase.setValue(Phase.ERROR);
    }

    private void lookUpOthers(@NonNull ScanResult r) {
        if (r.others == null || r.others.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (ScanResult.Other other : r.others) {
            if (other != null && other.name != null && !other.name.trim().isEmpty()) names.add(other.name);
        }
        otherLookup = Flowable.fromIterable(names)
                .concatMapMaybe(name -> pestRepo.matchByName(name)
                        .map(p -> new Object[]{name, p.id})
                        .onErrorComplete())
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pairs -> {
                    Map<String, Long> map = new HashMap<>();
                    for (Object[] p : pairs) map.put((String) p[0], (Long) p[1]);
                    otherPestIds.setValue(map);
                }, e -> { });
        add(otherLookup);
    }

    private void updateMatches() {
        matchedCrops.setValue(matchingCrops());
    }

    private List<HistoryItem> toHistory(List<ScanRecordEntity> list) {
        if (list == null) return Collections.emptyList();
        List<HistoryItem> out = new ArrayList<>(list.size());
        for (ScanRecordEntity e : list) out.add(new HistoryItem(e, ScanFormat.parse(gson, e.resultJson)));
        return out;
    }

    private static ScanMode parseMode(@Nullable String name) {
        if (name != null) {
            for (ScanMode m : ScanMode.values()) if (m.name().equals(name)) return m;
        }
        return ScanMode.PEST;
    }

    private void dispose(@Nullable Disposable d) {
        if (d != null) {
            d.dispose();
            disposables.delete(d);
        }
    }
}
