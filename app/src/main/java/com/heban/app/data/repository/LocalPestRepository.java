package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.PestEntity;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/** PestRepository 的本地实现（Room，数据由 MockDataImporter 导入）。M5 数据层实现前为占位。 */
public final class LocalPestRepository implements PestRepository {
    private final AppDatabase db;

    public LocalPestRepository(AppDatabase db) {
        this.db = db;
    }

    @Override
    public Flowable<List<PestEntity>> search(@Nullable String keyword, @Nullable String kind, @Nullable String crop) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<PestEntity>> seasonal(int month) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<Integer> count() {
        return Flowable.just(0);
    }

    @Override
    public Single<PestEntity> get(long id) {
        return db.pestDao().get(id);
    }

    @Override
    public Maybe<PestEntity> matchByName(String name) {
        return Maybe.empty();
    }

    @Override
    public Single<List<String>> cropOptions() {
        return Single.just(Collections.emptyList());
    }
}
