package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.PestEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * PestRepository 的本地实现（Room，数据由 MockDataImporter 导入，离线可用）。
 * 百科条目很少，查询全部后用 {@link PestQueries} 在内存中筛选；导入完成后 Flowable 会自动重新发射。
 */
public final class LocalPestRepository implements PestRepository {
    private final AppDatabase db;

    public LocalPestRepository(AppDatabase db) {
        this.db = db;
    }

    @Override
    public Flowable<List<PestEntity>> search(@Nullable String keyword, @Nullable String kind, @Nullable String crop) {
        return db.pestDao().observeAll()
                .map(all -> PestQueries.filter(all, keyword, kind, crop))
                .subscribeOn(Schedulers.io());
    }

    /** 本季高发（按当前月份；数据按江苏等长江中下游地区的发生规律编写） */
    @Override
    public Flowable<List<PestEntity>> seasonal(int month) {
        return db.pestDao().observeAll()
                .map(all -> PestQueries.seasonal(all, month))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<Integer> count() {
        return db.pestDao().observeCount().subscribeOn(Schedulers.io());
    }

    @Override
    public Single<PestEntity> get(long id) {
        return db.pestDao().get(id).subscribeOn(Schedulers.io());
    }

    @Override
    public Maybe<PestEntity> matchByName(String name) {
        return db.pestDao().getAll()
                .flatMapMaybe(all -> {
                    PestEntity p = PestQueries.match(all, name);
                    return p == null ? Maybe.<PestEntity>empty() : Maybe.just(p);
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<List<String>> cropOptions() {
        return db.pestDao().getAll()
                .map(PestQueries::cropOptions)
                .subscribeOn(Schedulers.io());
    }
}
