package com.heban.app.testutil;

import androidx.annotation.Nullable;

import com.heban.app.data.local.prefs.SessionStore;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

/** 内存版登录态（对应 DataStore 的 session.* 与 onboarding.done） */
public class FakeSessionStore implements SessionStore {
    public final BehaviorProcessor<Long> userId = BehaviorProcessor.createDefault(0L);
    @Nullable public String token;
    public boolean guest;
    public boolean onboardingDone;
    public int clearCount;

    public long userId() {
        return userId.getValue();
    }

    @Override
    public Flowable<Long> observeSessionUserId() {
        return userId.distinctUntilChanged();
    }

    @Override
    public Single<String> sessionToken() {
        return Single.fromCallable(() -> token == null ? "" : token);
    }

    @Override
    public Completable saveSession(long id, @Nullable String encryptedToken) {
        return Completable.fromAction(() -> {
            token = encryptedToken;
            guest = false;
            onboardingDone = true;
            userId.onNext(id);
        });
    }

    @Override
    public Completable clearSession() {
        return Completable.fromAction(() -> {
            clearCount++;
            token = null;
            guest = false;
            userId.onNext(0L);
        });
    }

    @Override
    public Completable saveGuest() {
        return Completable.fromAction(() -> {
            guest = true;
            onboardingDone = true;
        });
    }

    @Override
    public Completable saveOnboardingDone() {
        return Completable.fromAction(() -> onboardingDone = true);
    }
}
