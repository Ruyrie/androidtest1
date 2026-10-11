package com.heban.app.ui.crop;

import android.net.Uri;

import androidx.annotation.NonNull;
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
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.repository.CropRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** C03 / C04 ViewModel 测试用的假数据：作物仓库与照片复制。 */
final class AddCropFakes {
    private AddCropFakes() {
    }

    static PlotEntity plot(long id, String name, String type) {
        PlotEntity p = new PlotEntity();
        p.id = id;
        p.name = name;
        p.type = type;
        return p;
    }

    static CropCard card(long id, String name, String variety) {
        CropCard c = new CropCard();
        c.id = id;
        c.name = name;
        c.variety = variety;
        return c;
    }

    static final class Repo implements CropRepository {
        List<PlotEntity> plots = new ArrayList<>();
        List<CropCard> cards = new ArrayList<>();
        List<CropProfile> profiles = new ArrayList<>();
        CropEntity crop;
        final List<CropEntity> savedCrops = new ArrayList<>();
        final List<FarmRecordEntity> savedRecords = new ArrayList<>();
        final List<Boolean> syncFlags = new ArrayList<>();
        boolean failSave;

        @Override public Flowable<List<CropCard>> observeCropCards(@Nullable String filter) { return Flowable.just(cards); }
        @Override public Flowable<CropStats> observeCropStats() { return Flowable.just(new CropStats()); }
        @Override public Flowable<CropDetail> observeCropDetail(long cropId) { return Flowable.empty(); }

        @Override public Single<CropEntity> getCrop(long cropId) {
            return crop == null ? Single.error(new IllegalStateException("none")) : Single.just(crop);
        }

        @Override public Single<Long> saveCrop(CropEntity c) {
            if (failSave) return Single.error(new BusinessException("NOT_LOGGED_IN", "请先登录"));
            savedCrops.add(c);
            return Single.just(c.id == 0 ? 42L : c.id);
        }

        @Override public Completable deleteCrop(long cropId) { return Completable.complete(); }
        @Override public Completable dismissAttention(long cropId) { return Completable.complete(); }
        @Override public Completable markAttention(long cropId, String note, String source) { return Completable.complete(); }
        @Override public Single<List<CropEntity>> listCrops() { return Single.just(Collections.emptyList()); }
        @Override public Completable setWeatherAttention(long cropId, @Nullable String note) { return Completable.complete(); }

        @Override public CropProfile profileFor(String cropName) {
            CropProfile p = new CropProfile();
            p.name = cropName;
            p.expectedDays = "草莓".equals(cropName) ? 150 : 90;
            return p;
        }

        @Override public List<CropProfile> allProfiles() { return profiles; }
        @Override public Flowable<List<PlotEntity>> observePlots() { return Flowable.just(plots); }
        @Override public Flowable<List<PlotCard>> observePlotCards() { return Flowable.just(Collections.emptyList()); }
        @Override public Flowable<PlotStats> observePlotStats() { return Flowable.just(new PlotStats()); }
        @Override public Single<Long> savePlot(PlotEntity plot) { return Single.just(1L); }
        @Override public Completable deletePlot(long plotId) { return Completable.complete(); }
        @Override public Completable recordPlotReading(long plotId, double t, int h) { return Completable.complete(); }

        @Override public Single<Long> saveRecord(FarmRecordEntity record, boolean syncToLedger) {
            if (failSave) return Single.error(new BusinessException("NOT_LOGGED_IN", "请先登录"));
            savedRecords.add(record);
            syncFlags.add(syncToLedger);
            return Single.just(7L);
        }

        @Override public Completable deleteRecord(long recordId) { return Completable.complete(); }
    }

    /** 复制即返回 file:///copied/N.jpg；记录删除过的路径 */
    static final class Photos implements PhotoPickerStore {
        int counter;
        boolean fail;
        final List<String> deleted = new ArrayList<>();
        final List<Uri> discarded = new ArrayList<>();

        @NonNull
        @Override
        public Single<String> copy(@NonNull Uri source, @NonNull String dir) {
            if (fail) return Single.error(new java.io.IOException("x"));
            return Single.just("file:///copied/" + dir + "/" + (++counter) + ".jpg");
        }

        @NonNull
        @Override
        public Single<List<String>> copyAll(@NonNull List<Uri> sources, @NonNull String dir) {
            if (fail) return Single.error(new java.io.IOException("x"));
            List<String> out = new ArrayList<>();
            for (int i = 0; i < sources.size(); i++) out.add("file:///copied/" + dir + "/" + (++counter) + ".jpg");
            return Single.just(out);
        }

        @Override
        public void delete(@NonNull Collection<String> paths) {
            deleted.addAll(paths);
        }

        @Nullable
        @Override
        public Uri createCameraTarget() {
            return null;
        }

        @Override
        public void discardCameraTarget(@NonNull Uri target) {
            discarded.add(target);
        }
    }
}
