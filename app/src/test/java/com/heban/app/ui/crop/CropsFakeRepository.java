package com.heban.app.ui.crop;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;
import com.heban.app.data.repository.CropRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

/** 作物页面测试用的内存 CropRepository：按筛选返回卡片，记录写操作。 */
public class CropsFakeRepository implements CropRepository {
    public final BehaviorProcessor<List<CropCard>> cards = BehaviorProcessor.createDefault(new ArrayList<>());
    public final BehaviorProcessor<CropStats> stats = BehaviorProcessor.createDefault(new CropStats());
    public final BehaviorProcessor<List<PlotCard>> plotCards = BehaviorProcessor.createDefault(new ArrayList<>());
    public final BehaviorProcessor<PlotStats> plotStats = BehaviorProcessor.createDefault(new PlotStats());
    public final BehaviorProcessor<List<PlotEntity>> plots = BehaviorProcessor.createDefault(new ArrayList<>());
    public final Map<Long, BehaviorProcessor<CropDetail>> details = new HashMap<>();

    public final List<String> filterQueries = new ArrayList<>();
    public final List<Long> deletedCrops = new ArrayList<>();
    public final List<Long> dismissed = new ArrayList<>();
    public final List<Long> deletedRecords = new ArrayList<>();
    public final List<Long> deletedPlots = new ArrayList<>();
    public final List<PlotEntity> savedPlots = new ArrayList<>();
    public final List<double[]> readings = new ArrayList<>();
    @Nullable public Throwable writeError;

    public static CropCard card(long id, String name, String variety, String plot, double area, String mode,
                                String stage, int days, int expected, @Nullable String attention) {
        CropCard c = new CropCard();
        c.id = id;
        c.name = name;
        c.variety = variety;
        c.plotName = plot;
        c.areaMu = area;
        c.plantMode = mode;
        c.stage = stage;
        c.daysPlanted = days;
        c.expectedDays = expected;
        c.progress = expected > 0 ? Math.min(100, days * 100 / expected) : 0;
        c.attentionNote = attention;
        return c;
    }

    /** 与 LocalCropRepository 一致的筛选语义 */
    static boolean matches(CropCard c, @Nullable String filter) {
        if (filter == null || FILTER_ALL.equals(filter)) return true;
        if (FILTER_ATTENTION.equals(filter)) return c.attentionNote != null;
        return filter.equals(c.plantMode);
    }

    @Override
    public Flowable<List<CropCard>> observeCropCards(@Nullable String filter) {
        filterQueries.add(filter);
        return cards.map(list -> {
            List<CropCard> out = new ArrayList<>();
            for (CropCard c : list) if (matches(c, filter)) out.add(c);
            return out;
        });
    }

    @Override
    public Flowable<CropStats> observeCropStats() {
        return stats;
    }

    @Override
    public Flowable<CropDetail> observeCropDetail(long cropId) {
        BehaviorProcessor<CropDetail> p = details.get(cropId);
        return p == null ? Flowable.empty() : p;
    }

    @Override
    public Single<CropEntity> getCrop(long cropId) {
        return Single.error(new IllegalStateException());
    }

    @Override
    public Single<Long> saveCrop(CropEntity crop) {
        return Single.just(crop.id);
    }

    @Override
    public Completable deleteCrop(long cropId) {
        if (writeError != null) return Completable.error(writeError);
        deletedCrops.add(cropId);
        BehaviorProcessor<CropDetail> p = details.remove(cropId);
        if (p != null) p.onComplete();
        return Completable.complete();
    }

    @Override
    public Completable dismissAttention(long cropId) {
        if (writeError != null) return Completable.error(writeError);
        dismissed.add(cropId);
        return Completable.complete();
    }

    @Override
    public Completable markAttention(long cropId, String note, String source) {
        return Completable.complete();
    }

    @Override
    public Single<List<CropEntity>> listCrops() {
        return Single.just(Collections.emptyList());
    }

    @Override
    public Completable setWeatherAttention(long cropId, @Nullable String note) {
        return Completable.complete();
    }

    @Override
    public CropProfile profileFor(String cropName) {
        return new CropProfile();
    }

    @Override
    public List<CropProfile> allProfiles() {
        return Collections.emptyList();
    }

    @Override
    public Flowable<List<PlotEntity>> observePlots() {
        return plots;
    }

    @Override
    public Flowable<List<PlotCard>> observePlotCards() {
        return plotCards;
    }

    @Override
    public Flowable<PlotStats> observePlotStats() {
        return plotStats;
    }

    @Override
    public Single<Long> savePlot(PlotEntity plot) {
        if (writeError != null) return Single.error(writeError);
        savedPlots.add(plot);
        return Single.just(plot.id == 0 ? 99L : plot.id);
    }

    @Override
    public Completable deletePlot(long plotId) {
        deletedPlots.add(plotId);
        return Completable.complete();
    }

    @Override
    public Completable recordPlotReading(long plotId, double tempCelsius, int humidityPercent) {
        readings.add(new double[]{plotId, tempCelsius, humidityPercent});
        return Completable.complete();
    }

    @Override
    public Single<Long> saveRecord(FarmRecordEntity record, boolean syncToLedger) {
        return Single.just(1L);
    }

    @Override
    public Completable deleteRecord(long recordId) {
        deletedRecords.add(recordId);
        return Completable.complete();
    }
}
