package com.heban.app.data.local.prefs;

import androidx.annotation.Nullable;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 登录态读写（DataStore 键 session.user_id、session.token、session.guest、onboarding.done，技术准备文档表 7-2）。
 * 由 {@link SettingsStore} 实现；UserRepository 只依赖这个接口，便于在 JVM 单元测试中替换。
 */
public interface SessionStore {

    /** 当前登录用户 ID，未登录时为 0；值变化时发射 */
    Flowable<Long> observeSessionUserId();

    /** 已加密的会话 Token，没有时为 "" */
    Single<String> sessionToken();

    /**
     * 登录成功：session.user_id = userId、session.token = encryptedToken（为 null 时删除）、
     * session.guest = false、onboarding.done = true，一次写入。
     */
    Completable saveSession(long userId, @Nullable String encryptedToken);

    /** 退出登录：删除 session.user_id、session.token、session.guest */
    Completable clearSession();

    /** 游客浏览：session.guest = true、onboarding.done = true */
    Completable saveGuest();

    /** 引导页看完或跳过：onboarding.done = true */
    Completable saveOnboardingDone();
}
