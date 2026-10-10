package com.heban.app.data.repository;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.remote.sms.SmsService;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** mock 渠道的账号实现（M1 数据层子任务实现）。 */
public class MockUserRepository implements UserRepository {
    private final AppDatabase db;
    private final SettingsStore settings;
    private final SecureStore secureStore;
    private final SmsService sms;

    public MockUserRepository(AppDatabase db, SettingsStore settings, SecureStore secureStore, SmsService sms) {
        this.db = db;
        this.settings = settings;
        this.secureStore = secureStore;
        this.sms = sms;
    }

    @Override public Completable sendCode(String phone, SmsScene scene) { return sms.send(phone, scene); }
    @Override public Completable verifyCode(String phone, String code) { throw new UnsupportedOperationException("TODO M1"); }
    @Override public Single<Boolean> isRegistered(String phone) { throw new UnsupportedOperationException("TODO M1"); }
    @Override public Single<UserEntity> loginWithPassword(String phone, String password) { throw new UnsupportedOperationException("TODO M1"); }
    @Override public Single<UserEntity> loginWithSms(String phone, String code) { throw new UnsupportedOperationException("TODO M1"); }
    @Override public Single<UserEntity> register(RegisterForm form) { throw new UnsupportedOperationException("TODO M1"); }
    @Override public Completable resetPassword(String phone, String newPassword) { throw new UnsupportedOperationException("TODO M1"); }
    @Override public Single<String> submitAppeal(AppealEntity appeal) { throw new UnsupportedOperationException("TODO M1"); }
    @Override public Completable enterGuest() { return settings.put(SettingsStore.SESSION_GUEST, true); }
    @Override public Completable completeOnboarding() { return settings.put(SettingsStore.ONBOARDING_DONE, true); }
    @Override public Completable logout() { throw new UnsupportedOperationException("TODO M1"); }
    @Override public Flowable<UserEntity> currentUser() { return Flowable.empty(); }
    @Override public boolean isFakeSms() { return sms.isFake(); }
}
