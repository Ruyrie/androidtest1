package com.heban.app.data.repository;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.prefs.CurrentUser;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/** FavoriteRepository 的本地实现（Room）。M5 数据层实现前为占位。 */
public final class LocalFavoriteRepository implements FavoriteRepository {
    private final AppDatabase db;
    private final CurrentUser user;

    public LocalFavoriteRepository(AppDatabase db, CurrentUser user) {
        this.db = db;
        this.user = user;
    }

    @Override
    public Flowable<Boolean> observeIsFavorite(String targetType, long targetId) {
        return Flowable.just(false);
    }

    @Override
    public Completable toggle(String targetType, long targetId) {
        return Completable.complete();
    }

    @Override
    public Flowable<List<FavoriteEntity>> observeAll(String targetType) {
        return Flowable.just(Collections.emptyList());
    }
}
