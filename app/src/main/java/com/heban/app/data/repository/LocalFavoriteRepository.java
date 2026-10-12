package com.heban.app.data.repository;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.prefs.CurrentUser;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** FavoriteRepository 的本地实现（Room），按当前账号隔离；未登录时观察结果为 false / 空列表，toggle 报 NOT_LOGGED_IN。 */
public final class LocalFavoriteRepository implements FavoriteRepository {
    private final AppDatabase db;
    private final CurrentUser user;

    public LocalFavoriteRepository(AppDatabase db, CurrentUser user) {
        this.db = db;
        this.user = user;
    }

    @Override
    public Flowable<Boolean> observeIsFavorite(String targetType, long targetId) {
        return UserScope.observe(user, () -> false,
                uid -> db.favoriteDao().observeIsFavorite(uid, targetType, targetId));
    }

    @Override
    public Completable toggle(String targetType, long targetId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                db.runInTransaction(() -> {
                    if (db.favoriteDao().countSync(uid, targetType, targetId) > 0) {
                        db.favoriteDao().deleteSync(uid, targetType, targetId);
                    } else {
                        FavoriteEntity f = new FavoriteEntity();
                        f.userId = uid;
                        f.targetType = targetType;
                        f.targetId = targetId;
                        f.createdAt = System.currentTimeMillis();
                        db.favoriteDao().insertSync(f);
                    }
                }))).subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<List<FavoriteEntity>> observeAll(String targetType) {
        return UserScope.observe(user, Collections::<FavoriteEntity>emptyList,
                uid -> db.favoriteDao().observeAll(uid, targetType));
    }
}
