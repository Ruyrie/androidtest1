package com.heban.app.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** CropRepository 的本地实现（Room）。M2 数据层实现前为占位。 */
public final class LocalCropRepository implements CropRepository {
    private final Context app;
    private final AppDatabase db;
    private final CurrentUser user;
    private final Gson gson;

    public LocalCropRepository(Context app, AppDatabase db, CurrentUser user, Gson gson) {
        this.app = app;
        this.db = db;
        this.user = user;
        this.gson = gson;
    }

    @Override
    public Flowable<List<CropCard>> observeCropCards(@Nullable String filter) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<CropStats> observeCropStats() {
        return Flowable.just(new CropStats());
    }

    @Override
    public Flowable<CropDetail> observeCropDetail(long cropId) {
        return Flowable.empty();
    }

    @Override
    public Single<CropEntity> getCrop(long cropId) {
        return db.cropDao().get(cropId);
    }

    @Override
    public Single<Long> saveCrop(CropEntity crop) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable deleteCrop(long cropId) {
        return Completable.complete();
    }

    @Override
    public Completable dismissAttention(long cropId) {
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
        CropProfile p = new CropProfile();
        p.name = cropName;
        p.expectedDays = 90;
        p.icon = "eco";
        return p;
    }

    @Override
    public List<CropProfile> allProfiles() {
        return Collections.emptyList();
    }

    @Override
    public Flowable<List<PlotEntity>> observePlots() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<PlotCard>> observePlotCards() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<PlotStats> observePlotStats() {
        return Flowable.just(new PlotStats());
    }

    @Override
    public Single<Long> savePlot(PlotEntity plot) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable deletePlot(long plotId) {
        return Completable.complete();
    }

    @Override
    public Completable recordPlotReading(long plotId, double tempCelsius, int humidityPercent) {
        return Completable.complete();
    }

    @Override
    public Single<Long> saveRecord(FarmRecordEntity record, boolean syncToLedger) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable deleteRecord(long recordId) {
        return Completable.complete();
    }
}
