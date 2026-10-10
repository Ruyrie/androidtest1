package com.heban.app.data.repository;

import com.heban.app.data.local.entity.FavoriteEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/** 收藏（targetType 取 FavoriteEntity.TYPE_*）。按账号隔离；未登录时 toggle 抛出 NOT_LOGGED_IN。 */
public interface FavoriteRepository {
    Flowable<Boolean> observeIsFavorite(String targetType, long targetId);

    /** 切换收藏状态，返回切换后的状态由 observeIsFavorite 发射 */
    Completable toggle(String targetType, long targetId);

    Flowable<List<FavoriteEntity>> observeAll(String targetType);
}
