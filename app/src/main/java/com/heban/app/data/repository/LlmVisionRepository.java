package com.heban.app.data.repository;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.ScanRecordEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.model.ScanOutcome;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;

/** VisionRepository 的大模型实现（主模型 + 通义千问 VL 备用）。M4 数据层实现前为占位。 */
public final class LlmVisionRepository implements VisionRepository {
    private final Context app;
    private final OkHttpClient baseClient;
    private final Gson gson;
    private final AppDatabase db;
    private final SettingsStore settings;
    private final SecureStore secureStore;
    private final CurrentUser user;
    private final PestRepository pestRepository;
    private final CropRepository cropRepository;

    public LlmVisionRepository(Context app, OkHttpClient baseClient, Gson gson, AppDatabase db,
                               SettingsStore settings, SecureStore secureStore, CurrentUser user,
                               PestRepository pestRepository, CropRepository cropRepository) {
        this.app = app;
        this.baseClient = baseClient;
        this.gson = gson;
        this.db = db;
        this.settings = settings;
        this.secureStore = secureStore;
        this.user = user;
        this.pestRepository = pestRepository;
        this.cropRepository = cropRepository;
    }

    @Override
    public Single<ScanOutcome> recognize(Bitmap photo, ScanMode mode, @Nullable String cropHint) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Single<ScanOutcome> retry(long recordId) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Single<ScanOutcome> getOutcome(long recordId) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Flowable<List<ScanRecordEntity>> observeHistory() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Completable saveToCrop(long recordId, long cropId) {
        return Completable.complete();
    }

    @Override
    public Completable deleteRecord(long recordId) {
        return Completable.complete();
    }
}
