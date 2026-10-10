package com.heban.app.data.repository;

import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.remote.BusinessException;

import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** 按当前账号隔离数据：查询随登录态切换，未登录时返回空值；写操作要求已登录。 */
final class UserScope {
    static final String NOT_LOGGED_IN = "NOT_LOGGED_IN";

    private UserScope() {
    }

    /** 当前账号的查询；账号切换时重新查询，未登录 / 游客（userId 0）时发射 empty */
    static <T> Flowable<T> observe(CurrentUser user, Supplier<T> empty, Function<Long, Publisher<T>> query) {
        return user.observeUserId()
                .switchMap(uid -> uid > 0 ? Flowable.fromPublisher(query.apply(uid)) : Flowable.just(empty.get()))
                .subscribeOn(Schedulers.io());
    }

    /** 当前账号 id（已切到 io 线程）；未登录时报 BusinessException("NOT_LOGGED_IN") */
    static Single<Long> require(CurrentUser user) {
        return user.userId()
                .observeOn(Schedulers.io())
                .flatMap(uid -> uid > 0 ? Single.just(uid) : Single.error(notLoggedIn()));
    }

    static BusinessException notLoggedIn() {
        return new BusinessException(NOT_LOGGED_IN, "请先登录");
    }

    static BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message);
    }
}
