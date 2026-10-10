package com.heban.app.ui.account;

import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.RegisterForm;
import com.heban.app.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 单元测试用的 UserRepository：记录调用，返回值可配置。 */
class FakeUserRepository implements UserRepository {
    final List<String> calls = new ArrayList<>();

    Completable sendResult = Completable.complete();
    Completable verifyResult = Completable.complete();
    Completable resetResult = Completable.complete();
    Single<String> appealResult = Single.just("HB20261010-0832");
    boolean fakeSms;

    String lastPhone;
    SmsScene lastScene;
    String lastCode;
    String lastPassword;
    AppealEntity lastAppeal;

    @Override
    public Completable sendCode(String phone, SmsScene scene) {
        calls.add("sendCode");
        lastPhone = phone;
        lastScene = scene;
        return sendResult;
    }

    @Override
    public Completable verifyCode(String phone, String code) {
        calls.add("verifyCode");
        lastPhone = phone;
        lastCode = code;
        return verifyResult;
    }

    @Override
    public Single<Boolean> isRegistered(String phone) {
        calls.add("isRegistered");
        return Single.just(true);
    }

    @Override
    public Single<UserEntity> loginWithPassword(String phone, String password) {
        return Single.never();
    }

    @Override
    public Single<UserEntity> loginWithSms(String phone, String code) {
        return Single.never();
    }

    @Override
    public Single<UserEntity> register(RegisterForm form) {
        return Single.never();
    }

    @Override
    public Completable resetPassword(String phone, String newPassword) {
        calls.add("resetPassword");
        lastPhone = phone;
        lastPassword = newPassword;
        return resetResult;
    }

    @Override
    public Single<String> submitAppeal(AppealEntity appeal) {
        calls.add("submitAppeal");
        lastAppeal = appeal;
        return appealResult;
    }

    @Override
    public Completable enterGuest() {
        return Completable.complete();
    }

    @Override
    public Completable completeOnboarding() {
        return Completable.complete();
    }

    @Override
    public Completable logout() {
        return Completable.complete();
    }

    @Override
    public Flowable<UserEntity> currentUser() {
        return Flowable.empty();
    }

    @Override
    public boolean isFakeSms() {
        return fakeSms;
    }

    int count(String call) {
        int n = 0;
        for (String c : calls) if (c.equals(call)) n++;
        return n;
    }

    @Override
    public Completable updateProfile(UserEntity profile) {
        return Completable.complete();
    }

    @Override
    public Completable changePhone(String newPhone) {
        return Completable.complete();
    }

    @Override
    public Completable deleteAccount() {
        return Completable.complete();
    }
}
