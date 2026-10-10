package com.heban.app.data.repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.JsonParseException;
import com.heban.app.data.local.dao.UserDao;
import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.local.prefs.SessionStore;
import com.heban.app.data.local.prefs.TokenCipher;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.api.AuthDtos;
import com.heban.app.data.remote.api.BaseResponse;
import com.heban.app.data.remote.api.HeBanApi;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.util.PasswordStrength;
import com.heban.app.util.PhoneMask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

/**
 * remote 渠道的账号实现：调用禾伴后端接口（技术准备文档 8.4）。验证码由后端发送和校验；
 * 登录返回的 Token 经 SecureStore 加密后写入 session.token；用户资料缓存到 Room 的 user 表。
 */
public class RemoteUserRepository implements UserRepository {
    private static final String TAG = "RemoteUserRepository";

    private final HeBanApi api;
    private final UserDao userDao;
    private final SessionStore session;
    private final TokenCipher cipher;
    /** verifyCode 通过的验证码（手机号 → 验证码），注册、重置密码、申诉时一并提交供后端复核 */
    private final Map<String, String> verifiedCodes = new ConcurrentHashMap<>();

    public RemoteUserRepository(HeBanApi api, UserDao userDao, SessionStore session, TokenCipher cipher) {
        this.api = api;
        this.userDao = userDao;
        this.session = session;
        this.cipher = cipher;
    }

    // —— 验证码 ——

    @Override
    public Completable sendCode(String phone, SmsScene scene) {
        return Completable.defer(() -> {
            if (!PhoneMask.isValid(phone)) return Completable.error(invalidPhone());
            return api.sendSmsCode(new AuthDtos.SmsCodeReq(phone, scene.name()))
                    .doOnSuccess(BaseResponse::check)
                    .ignoreElement();
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable verifyCode(String phone, String code) {
        return Completable.defer(() -> {
            if (code == null || code.trim().isEmpty()) {
                return Completable.error(new BusinessException(Errors.CODE_INVALID, "验证码错误或已过期"));
            }
            String c = code.trim();
            return api.verifySmsCode(new AuthDtos.VerifyCodeReq(phone, c))
                    .doOnSuccess(BaseResponse::check)
                    .doOnSuccess(r -> verifiedCodes.put(phone, c))
                    .ignoreElement();
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Boolean> isRegistered(String phone) {
        return Single.defer(() -> api.isRegistered(phone))
                .map(BaseResponse::dataOrThrow)
                .subscribeOn(Schedulers.io());
    }

    // —— 登录 / 注册 ——

    @Override
    public Single<UserEntity> loginWithPassword(String phone, String password) {
        return Single.defer(() -> api.login(new AuthDtos.LoginReq(phone, password)))
                .map(BaseResponse::dataOrThrow)
                .flatMap(this::saveAuth)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<UserEntity> loginWithSms(String phone, String code) {
        return Single.defer(() -> api.loginSms(new AuthDtos.LoginSmsReq(phone, code)))
                .map(BaseResponse::dataOrThrow)
                .flatMap(this::saveAuth)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<UserEntity> register(RegisterForm form) {
        return Single.defer(() -> {
            if (!PasswordStrength.evaluate(form.password).valid()) {
                return Single.<UserEntity>error(weakPassword());
            }
            return api.register(AuthDtos.RegisterReq.from(form, verifiedCodes.get(form.phone)))
                    .map(BaseResponse::dataOrThrow)
                    .flatMap(this::saveAuth)
                    .doOnSuccess(u -> verifiedCodes.remove(form.phone));
        }).subscribeOn(Schedulers.io());
    }

    // —— 找回密码 / 申诉 ——

    @Override
    public Completable resetPassword(String phone, String newPassword) {
        return Completable.defer(() -> {
            if (!PasswordStrength.evaluate(newPassword).valid()) {
                return Completable.error(weakPassword());
            }
            return api.resetPassword(new AuthDtos.ResetPasswordReq(phone, verifiedCodes.get(phone), newPassword))
                    .doOnSuccess(BaseResponse::check)
                    .ignoreElement()
                    .doOnComplete(() -> verifiedCodes.remove(phone))
                    .andThen(Completable.defer(session::clearSession));
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String> submitAppeal(AppealEntity appeal) {
        return Single.defer(() -> {
            String newPhone = appeal.newPhone.trim();
            if (newPhone.equals(appeal.oldPhone.trim())) {
                return Single.<String>error(new BusinessException(Errors.SAME_PHONE, "新手机号不能与原手机号相同"));
            }
            return api.appeal(AuthDtos.AppealReq.from(appeal, verifiedCodes.get(newPhone)))
                    .map(BaseResponse::dataOrThrow)
                    .map(r -> {
                        if (r.appealNo == null) throw new JsonParseException("appealNo 为空");
                        verifiedCodes.remove(newPhone);
                        return r.appealNo;
                    });
        }).subscribeOn(Schedulers.io());
    }

    // —— 登录态 ——

    @Override
    public Completable enterGuest() {
        return Completable.defer(session::saveGuest).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable completeOnboarding() {
        return Completable.defer(session::saveOnboardingDone).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable logout() {
        return Completable.defer(session::clearSession).subscribeOn(Schedulers.io());
    }

    /** 先发射 Room 中的缓存，同时请求 GET /users/me 刷新缓存；401 时清除登录态 */
    @Override
    public Flowable<UserEntity> currentUser() {
        return session.observeSessionUserId()
                .switchMap(id -> id > 0
                        ? userDao.observe(id).mergeWith(refreshMe())
                        : Flowable.<UserEntity>empty())
                .subscribeOn(Schedulers.io());
    }

    /** 验证码由后端发送，界面不显示「模拟短信」提示 */
    @Override
    public Completable updateProfile(UserEntity profile) {
        return Completable.error(new UnsupportedOperationException());
    }

    @Override
    public Completable changePhone(String newPhone) {
        return Completable.error(new UnsupportedOperationException());
    }

    @Override
    public Completable deleteAccount() {
        return Completable.error(new UnsupportedOperationException());
    }

    @Override
    public boolean isFakeSms() {
        return false;
    }

    // —— 内部 ——

    private Single<UserEntity> saveAuth(AuthDtos.AuthResp r) {
        if (r.user == null) return Single.error(new JsonParseException("user 为空"));
        UserEntity u = r.user.toEntity();
        return cacheUser(u)
                .andThen(Completable.defer(() -> session.saveSession(u.id, encrypt(r.token))))
                .toSingleDefault(u);
    }

    private Completable refreshMe() {
        return session.sessionToken()
                .flatMap(token -> api.me(bearer(token)))
                .map(BaseResponse::dataOrThrow)
                .flatMapCompletable(dto -> cacheUser(dto.toEntity()))
                .onErrorResumeNext(e -> {
                    if (e instanceof HttpException && ((HttpException) e).code() == 401) {
                        return session.clearSession();
                    }
                    Log.w(TAG, "刷新用户资料失败，继续使用缓存", e);
                    return Completable.complete();
                });
    }

    /** 以服务端 id 为主键写入 user 表：已有则更新，没有则插入；缓存失败不影响登录 */
    private Completable cacheUser(UserEntity u) {
        return userDao.get(u.id)
                .flatMapCompletable(old -> userDao.update(u))
                .onErrorResumeNext(e -> userDao.insert(u).ignoreElement())
                .onErrorComplete();
    }

    @Nullable
    private String encrypt(@Nullable String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            return cipher.encrypt(token);
        } catch (Exception e) {
            Log.w(TAG, "Token 加密失败，跳过保存", e);
            return null;
        }
    }

    @Nullable
    private String bearer(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isEmpty()) return null;
        try {
            return "Bearer " + cipher.decrypt(encryptedToken);
        } catch (Exception e) {
            return null;
        }
    }

    private static BusinessException invalidPhone() {
        return new BusinessException(Errors.INVALID_PHONE, "请输入正确的手机号");
    }

    private static BusinessException weakPassword() {
        return new BusinessException(Errors.WEAK_PASSWORD, "密码需为 8–20 位，并同时包含字母和数字");
    }
}
