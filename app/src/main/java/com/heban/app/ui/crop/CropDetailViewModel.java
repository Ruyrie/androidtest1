package com.heban.app.ui.crop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * C02 作物详情：作物、地块、生长阶段与农事记录（CropRepository.observeCropDetail），
 * 「生长数据」Tab 按记录类型汇总次数与投入，「照片」Tab 汇总各条记录的现场照片。
 */
public class CropDetailViewModel extends BaseViewModel {
    public static final int TAB_RECORDS = 0;
    public static final int TAB_GROWTH = 1;
    public static final int TAB_PHOTOS = 2;

    /** 生长数据：某一类农事的次数、投入与最近一次日期 */
    public static final class TypeStat {
        public final FarmType type;
        public int count;
        public long costCent;
        public long lastAt;

        TypeStat(FarmType type) {
            this.type = type;
        }
    }

    private final CropRepository repo;
    private final Gson gson;
    private long cropId;
    private boolean started;
    private boolean closing;

    public final MutableLiveData<CropDetail> detail = new MutableLiveData<>();
    public final MutableLiveData<Integer> tab = new MutableLiveData<>(TAB_RECORDS);
    public final MutableLiveData<List<FarmRecordEntity>> records = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<List<String>> photos = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<List<TypeStat>> typeStats = new MutableLiveData<>(Collections.emptyList());
    /** 全部记录的投入合计（分） */
    public final MutableLiveData<Long> totalCost = new MutableLiveData<>(0L);
    public final MutableLiveData<Boolean> loaded = new MutableLiveData<>(false);
    public final SingleLiveEvent<Integer> toastRes = new SingleLiveEvent<>();
    /** 作物已删除或不存在：关闭页面 */
    public final SingleLiveEvent<Boolean> close = new SingleLiveEvent<>();

    public CropDetailViewModel(AppContainer c) {
        this(c, c.cropRepository, c.gson);
    }

    CropDetailViewModel(@Nullable AppContainer c, @NonNull CropRepository repo, @NonNull Gson gson) {
        super(c);
        this.repo = repo;
        this.gson = gson;
    }

    public void start(long id) {
        if (started) return;
        started = true;
        cropId = id;
        add(repo.observeCropDetail(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDetail, this::onError, () -> {
                    // 作物不存在（或刚被删除）时完成而不再发射
                    if (detail.getValue() == null && !closing) toastRes.setValue(R.string.crop_detail_not_found);
                    closePage();
                }));
    }

    public long cropId() {
        return cropId;
    }

    public void selectTab(int t) {
        Integer cur = tab.getValue();
        if (cur == null || cur != t) tab.setValue(t);
    }

    public void dismissAttention() {
        add(repo.dismissAttention(cropId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> toastRes.setValue(R.string.crop_detail_attention_dismissed), this::onError));
    }

    public void deleteCrop() {
        add(repo.deleteCrop(cropId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::closePage, this::onError));
    }

    public void deleteRecord(long recordId) {
        add(repo.deleteRecord(recordId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> toastRes.setValue(R.string.crop_detail_record_deleted), this::onError));
    }

    private void closePage() {
        if (closing) return;
        closing = true;
        close.setValue(true);
    }

    private void onDetail(CropDetail d) {
        detail.setValue(d);
        List<FarmRecordEntity> list = d.records == null ? Collections.emptyList() : d.records;
        records.setValue(list);
        photos.setValue(collectPhotos(gson, list));
        long[] total = new long[1];
        typeStats.setValue(summarize(list, total));
        totalCost.setValue(total[0]);
        loaded.setValue(true);
    }

    /** 各条记录的照片（记录按日期倒序，照片保持记录内顺序） */
    @NonNull
    static List<String> collectPhotos(@NonNull Gson gson, @NonNull List<FarmRecordEntity> records) {
        List<String> out = new ArrayList<>();
        for (FarmRecordEntity r : records) out.addAll(CropsFormat.parsePhotos(gson, r.photos));
        return out;
    }

    /** 按 FarmType 顺序汇总有记录的类型；totalOut[0] 返回投入合计 */
    @NonNull
    static List<TypeStat> summarize(@NonNull List<FarmRecordEntity> records, @NonNull long[] totalOut) {
        Map<FarmType, TypeStat> map = new EnumMap<>(FarmType.class);
        long total = 0;
        for (FarmRecordEntity r : records) {
            FarmType t = FarmType.of(r.type);
            TypeStat s = map.get(t);
            if (s == null) {
                s = new TypeStat(t);
                map.put(t, s);
            }
            s.count++;
            s.costCent += Math.max(0, r.costCent);
            s.lastAt = Math.max(s.lastAt, r.date);
            total += Math.max(0, r.costCent);
        }
        totalOut[0] = total;
        return new ArrayList<>(map.values());
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }

    /**
     * 「问 AI 诊断」带入的问题：「我种的番茄（樱桃番茄，结果期第 68 天，1号大棚）……」；
     * 有「需关注」提示时请 AI 针对该风险诊断，否则请教当前阶段的管理要点。
     */
    @Nullable
    static String aiPrompt(@Nullable CropDetail d) {
        if (d == null || d.crop == null) return null;
        StringBuilder who = new StringBuilder(d.crop.name);
        StringBuilder info = new StringBuilder();
        if (d.crop.variety != null && !d.crop.variety.trim().isEmpty()) info.append(d.crop.variety.trim());
        if (d.card != null) {
            if (info.length() > 0) info.append("，");
            if (d.card.stage != null && !d.card.stage.isEmpty()) info.append(d.card.stage);
            info.append("第 ").append(d.card.daysPlanted).append(" 天");
        }
        if (d.plotName != null && !d.plotName.isEmpty()) {
            if (info.length() > 0) info.append("，");
            info.append(d.plotName);
        }
        if (info.length() > 0) who.append("（").append(info).append("）");
        String note = d.crop.attentionNote;
        if (note != null && !note.trim().isEmpty()) {
            return "我种的" + who + "出现了「" + note.trim() + "」提示，请帮我诊断原因并给出防治建议。";
        }
        return "我种的" + who + "现在需要注意什么？请结合当前生长阶段给出管理建议。";
    }
}
