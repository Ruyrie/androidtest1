package com.heban.app.ui.account;

import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.RegisterForm;
import com.heban.app.data.repository.UserRepository;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** A03 / A04 ViewModel 测试用的 UserRepository：结果可配置，并记录调用次数和参数。 */
class LoginFakeUserRepository implements UserRepository {
    Single<UserEntity> loginResult = Single.just(new UserEntity());
    Completable sendCodeResult = Completable.complete();
    Completable guestResult = Completable.complete();
    boolean fakeSms = true;

    int loginWithPasswordCalls;
    int loginWithSmsCalls;
    int sendCodeCalls;
    int enterGuestCalls;
    String lastPhone;
    String lastPassword;
    String lastCode;
    SmsScene lastScene;

    @Override
    public Completable sendCode(String phone, SmsScene scene) {
        sendCodeCalls++;
        lastPhone = phone;
        lastScene = scene;
        return sendCodeResult;
    }

    @Override
    public Single<UserEntity> loginWithPassword(String phone, String password) {
        loginWithPasswordCalls++;
        lastPhone = phone;
        lastPassword = password;
        return loginResult;
    }

    @Override
    public Single<UserEntity> loginWithSms(String phone, String code) {
        loginWithSmsCalls++;
        lastPhone = phone;
        lastCode = code;
        return loginResult;
    }

    @Override
    public Completable enterGuest() {
        enterGuestCalls++;
        return guestResult;
    }

    @Override
    public boolean isFakeSms() {
        return fakeSms;
    }

    @Override
    public Completable verifyCode(String phone, String code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Single<Boolean> isRegistered(String phone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Single<UserEntity> register(RegisterForm form) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Completable resetPassword(String phone, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Single<String> submitAppeal(AppealEntity appeal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Completable completeOnboarding() {
        return Completable.complete();
    }

    @Override
    public Completable logout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flowable<UserEntity> currentUser() {
        return Flowable.empty();
    }
}
