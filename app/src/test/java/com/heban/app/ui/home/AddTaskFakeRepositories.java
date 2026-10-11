package com.heban.app.ui.home;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CityOption;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;
import com.heban.app.data.model.WeatherState;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.WeatherRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** H04 测试用的作物 / 天气假数据。 */
final class AddTaskFakeRepositories {
    private AddTaskFakeRepositories() {
    }

    static CropCard card(long id, String name, String plot) {
        CropCard c = new CropCard();
        c.id = id;
        c.name = name;
        c.plotName = plot;
        return c;
    }

    static final class Crops implements CropRepository {
        List<CropCard> cards = new ArrayList<>();

        @Override
        public Flowable<List<CropCard>> observeCropCards(@Nullable String filter) {
            return Flowable.just(cards);
        }

        @Override
        public Flowable<CropStats> observeCropStats() {
            return Flowable.empty();
        }

        @Override
        public Flowable<CropDetail> observeCropDetail(long cropId) {
            return Flowable.empty();
        }

        @Override
        public Single<CropEntity> getCrop(long cropId) {
            return Single.never();
        }

        @Override
        public Single<Long> saveCrop(CropEntity crop) {
            return Single.just(1L);
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
            return null;
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
            return Flowable.empty();
        }

        @Override
        public Single<Long> savePlot(PlotEntity plot) {
            return Single.just(1L);
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
            return Single.just(1L);
        }

        @Override
        public Completable deleteRecord(long recordId) {
            return Completable.complete();
        }
    }

    static final class Weather implements WeatherRepository {
        final List<String> requests = new ArrayList<>();
        String tip = "";

        @Override
        public Flowable<WeatherState> observe() {
            return Flowable.empty();
        }

        @Override
        public Completable refresh(boolean force) {
            return Completable.complete();
        }

        @Override
        public Single<List<CityOption>> searchCity(String keyword) {
            return Single.just(Collections.emptyList());
        }

        @Override
        public Completable setLocation(CityOption city) {
            return Completable.complete();
        }

        @Override
        public Single<String> farmTaskTip(FarmType type, LocalDate day) {
            requests.add(type.name() + "@" + day);
            return Single.just(tip);
        }
    }
}
