package com.heban.app.data.repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.heban.app.data.local.dao.AppealDao;
import com.heban.app.data.local.dao.UserDao;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SessionStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.local.prefs.TokenCipher;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.remote.sms.SmsService;
import com.heban.app.util.PasswordHasher;
import com.heban.app.util.PasswordStrength;
import com.heban.app.util.PhoneMask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.function.LongSupplier;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * mock 渠道的账号实现（技术准备文档 7.1、7.4、12.1）：账号存 Room 的 user 表，密码只存 PBKDF2 哈希与盐；
 * 短信走 SmsService（阿里云或模拟）；登录态写 DataStore，模拟 Token 经 SecureStore 加密后保存。
 */
public class MockUserRepository implements UserRepository {
    private static final String TAG = "MockUserRepository";

    /** 连续输错 5 次锁定 */
    static final int MAX_FAILED_ATTEMPTS = 5;
    /** 锁定 10 分钟 */
    static final long LOCK_MILLIS = 10 * 60_000L;
    /** 注销后 7 天内重新登录可撤销 */
    static final long RESTORE_WINDOW_MILLIS = 7 * 24 * 60 * 60_000L;
    static final String DEFAULT_NICKNAME_PREFIX = "农友";

    static final String MSG_INVALID_PHONE = "请输入正确的手机号";
    static final String MSG_PHONE_EXISTS = "该手机号已注册，请直接登录";
    static final String MSG_NOT_REGISTERED = "该手机号未注册";
    static final String MSG_CODE_INVALID = "验证码错误或已过期";
    static final String MSG_NO_PASSWORD = "该账号未设置密码，请使用验证码登录";
    static final String MSG_WEAK_PASSWORD = "密码需为 8–20 位，并同时包含字母和数字";
    static final String MSG_SAME_AS_RECENT = "新密码不能与最近使用过的密码相同";
    static final String MSG_SAME_PHONE = "新手机号不能与原手机号相同";

    private final UserDao userDao;
    private final AppealDao appealDao;
    private final SessionStore session;
    private final TokenCipher cipher;
    private final SmsService sms;
    private final LongSupplier clock;
    private final Random random = new Random();

    public MockUserRepository(AppDatabase db, SettingsStore settings, SecureStore secureStore, SmsService sms) {
        this(db.userDao(), db.appealDao(), settings, secureStore, sms, System::currentTimeMillis);
    }

    /** clock 返回当前毫秒时间（单元测试中可控制锁定与注销期限） */
    public MockUserRepository(UserDao userDao, AppealDao appealDao, SessionStore session,
                              TokenCipher cipher, SmsService sms, LongSupplier clock) {
        this.userDao = userDao;
        this.appealDao = appealDao;
        this.session = session;
        this.cipher = cipher;
        this.sms = sms;
        this.clock = clock;
    }

    // —— 验证码 ——

    @Override
    public Completable sendCode(String phone, SmsScene scene) {
        return Completable.defer(() -> {
            if (!PhoneMask.isValid(phone)) return Completable.error(invalidPhone());
            if (scene != SmsScene.REGISTER && scene != SmsScene.RESET_PASSWORD) {
                return sms.send(phone, scene);
            }
            return registered(phone).flatMapCompletable(exists -> {
                if (scene == SmsScene.REGISTER && exists) {
                    return Completable.error(error(Errors.PHONE_EXISTS, MSG_PHONE_EXISTS));
                }
                if (scene == SmsScene.RESET_PASSWORD && !exists) {
                    return Completable.error(notRegistered());
                }
                return sms.send(phone, scene);
            });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable verifyCode(String phone, String code) {
        return Completable.defer(() -> {
            if (code == null || code.trim().isEmpty()) return Completable.error(codeInvalid());
            return sms.verify(phone, code.trim()).flatMapCompletable(ok -> ok
                    ? Completable.complete() : Completable.error(codeInvalid()));
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Boolean> isRegistered(String phone) {
        return registered(phone).subscribeOn(Schedulers.io());
    }

    // —— 登录 / 注册 ——

    @Override
    public Single<UserEntity> loginWithPassword(String phone, String password) {
        return Single.defer(() -> {
            if (!PhoneMask.isValid(phone)) return Single.error(invalidPhone());
            return findAccount(phone)
                    .switchIfEmpty(Single.error(this::notRegistered))
                    .flatMap(u -> checkPassword(u, password));
        }).subscribeOn(Schedulers.io());
    }

    private Single<UserEntity> checkPassword(UserEntity u, String password) {
        long now = clock.getAsLong();
        if (u.lockedUntil > now) {
            return Single.error(locked(u.lockedUntil - now));
        }
        if (isBlank(u.passwordHash) || isBlank(u.salt)) {
            return Single.error(error(Errors.NO_PASSWORD, MSG_NO_PASSWORD));
        }
        if (!PasswordHasher.verify(password, u.salt, u.passwordHash)) {
            BusinessException e;
            u.failedAttempts++;
            if (u.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // 锁定后计数清零，解锁后重新有 5 次机会
                u.failedAttempts = 0;
                u.lockedUntil = now + LOCK_MILLIS;
                e = locked(LOCK_MILLIS);
            } else {
                e = error(Errors.WRONG_PASSWORD,
                        "账号或密码错误，还可尝试 " + (MAX_FAILED_ATTEMPTS - u.failedAttempts) + " 次");
            }
            return userDao.update(u).andThen(Single.<UserEntity>error(e));
        }
        u.failedAttempts = 0;
        u.lockedUntil = 0;
        u.deletedAt = 0;   // 注销 7 天内重新登录：撤销注销
        return userDao.update(u).andThen(writeSession(u)).toSingleDefault(u);
    }

    @Override
    public Single<UserEntity> loginWithSms(String phone, String code) {
        return Single.defer(() -> {
            if (!PhoneMask.isValid(phone)) return Single.error(invalidPhone());
            return verifyCode(phone, code)
                    .andThen(findAccount(phone)
                            .flatMapSingle(this::restoreIfDeleted)
                            .switchIfEmpty(Single.defer(() -> saveNewUser(newSmsUser(phone)))))
                    .flatMap(u -> writeSession(u).toSingleDefault(u));
        }).subscribeOn(Schedulers.io());
    }

    private Single<UserEntity> restoreIfDeleted(UserEntity u) {
        if (u.deletedAt == 0) return Single.just(u);
        u.deletedAt = 0;
        return userDao.update(u).toSingleDefault(u);
    }

    /** 验证码登录自动创建的账号：默认昵称「农友 + 手机号后四位」，没有密码 */
    private UserEntity newSmsUser(String phone) {
        UserEntity u = new UserEntity();
        u.phone = phone;
        u.nickname = defaultNickname(phone);
        u.createdAt = clock.getAsLong();
        return u;
    }

    @Override
    public Single<UserEntity> register(RegisterForm form) {
        return Single.defer(() -> {
            String phone = form.phone;
            if (!PhoneMask.isValid(phone)) return Single.error(invalidPhone());
            return registered(phone).flatMap(exists -> {
                if (exists) return Single.<UserEntity>error(error(Errors.PHONE_EXISTS, MSG_PHONE_EXISTS));
                if (!PasswordStrength.evaluate(form.password).valid()) {
                    return Single.<UserEntity>error(weakPassword());
                }
                UserEntity u = new UserEntity();
                u.phone = phone;
                u.salt = PasswordHasher.newSalt();
                u.passwordHash = PasswordHasher.hash(form.password, u.salt);
                u.nickname = isBlank(form.nickname) ? defaultNickname(phone) : form.nickname.trim();
                u.region = trimToNull(form.region);
                u.identity = trimToNull(form.identity);
                u.crops = joinCrops(form.crops);
                u.createdAt = clock.getAsLong();
                return saveNewUser(u);
            }).flatMap(u -> writeSession(u).toSingleDefault(u));
        }).subscribeOn(Schedulers.io());
    }

    // —— 找回密码 / 申诉 ——

    @Override
    public Completable resetPassword(String phone, String newPassword) {
        return Single.defer(() -> findAccount(phone).switchIfEmpty(Single.error(this::notRegistered)))
                .flatMapCompletable(u -> {
                    if (!PasswordStrength.evaluate(newPassword).valid()) {
                        return Completable.error(weakPassword());
                    }
                    if (PasswordHasher.verify(newPassword, u.salt, u.passwordHash)
                            || PasswordHasher.verify(newPassword, u.prevSalt, u.prevPasswordHash)) {
                        return Completable.error(error(Errors.SAME_AS_RECENT, MSG_SAME_AS_RECENT));
                    }
                    if (u.passwordHash != null) {
                        u.prevPasswordHash = u.passwordHash;
                        u.prevSalt = u.salt;
                    }
                    u.salt = PasswordHasher.newSalt();
                    u.passwordHash = PasswordHasher.hash(newPassword, u.salt);
                    u.failedAttempts = 0;
                    u.lockedUntil = 0;
                    return userDao.update(u);
                })
                .andThen(Completable.defer(session::clearSession))   // 其他设备上的登录失效
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String> submitAppeal(AppealEntity appeal) {
        return Single.defer(() -> {
            String oldPhone = appeal.oldPhone.trim();
            String newPhone = appeal.newPhone.trim();
            if (newPhone.equals(oldPhone)) {
                return Single.<String>error(error(Errors.SAME_PHONE, MSG_SAME_PHONE));
            }
            long now = clock.getAsLong();
            appeal.oldPhone = oldPhone;
            appeal.newPhone = newPhone;
            appeal.appealNo = newAppealNo(now);
            appeal.createdAt = now;
            appeal.status = AppealEntity.STATUS_PENDING;
            return appealDao.insert(appeal).map(id -> {
                appeal.id = id;
                return appeal.appealNo;
            });
        }).subscribeOn(Schedulers.io());
    }

    /** 申诉编号：HB + yyyyMMdd + "-" + 4 位随机数字，如 HB20261010-0832 */
    String newAppealNo(long now) {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(now));
        return "HB" + date + "-" + String.format(Locale.US, "%04d", random.nextInt(10000));
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

    @Override
    public Flowable<UserEntity> currentUser() {
        return session.observeSessionUserId()
                .switchMap(id -> id > 0 ? userDao.observe(id) : Flowable.<UserEntity>empty())
                .subscribeOn(Schedulers.io());
    }

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
        return sms.isFake();
    }

    // —— 内部 ——

    /** 按手机号查找账号；注销超过 7 天的记录视为未注册 */
    private Maybe<UserEntity> findAccount(String phone) {
        return userDao.findByPhone(phone)
                .filter(u -> u.deletedAt == 0 || clock.getAsLong() - u.deletedAt <= RESTORE_WINDOW_MILLIS);
    }

    private Single<Boolean> registered(String phone) {
        return findAccount(phone).map(u -> true).defaultIfEmpty(false);
    }

    /** 插入新账号；手机号还留有注销超过 7 天的旧记录时，用新账号覆盖该行（phone 列唯一） */
    private Single<UserEntity> saveNewUser(UserEntity fresh) {
        return userDao.findByPhone(fresh.phone)
                .flatMapSingle(stale -> {
                    fresh.id = stale.id;
                    return userDao.update(fresh).toSingleDefault(fresh);
                })
                .switchIfEmpty(Single.defer(() -> userDao.insert(fresh).map(id -> {
                    fresh.id = id;
                    return fresh;
                })));
    }

    /** 写入登录态；Token 加密失败时不保存 Token，但仍然登录 */
    private Completable writeSession(UserEntity u) {
        return Completable.defer(() -> session.saveSession(u.id, newEncryptedToken()));
    }

    @Nullable
    private String newEncryptedToken() {
        try {
            return cipher.encrypt(UUID.randomUUID().toString());
        } catch (Exception e) {
            Log.w(TAG, "模拟 Token 加密失败，跳过保存", e);
            return null;
        }
    }

    static String defaultNickname(String phone) {
        return DEFAULT_NICKNAME_PREFIX + (phone.length() > 4 ? phone.substring(phone.length() - 4) : phone);
    }

    @Nullable
    static String joinCrops(@Nullable List<String> crops) {
        if (crops == null) return null;
        List<String> names = new ArrayList<>();
        for (String c : crops) {
            if (!isBlank(c)) names.add(c.trim());
        }
        return names.isEmpty() ? null : String.join(",", names);
    }

    private static boolean isBlank(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }

    @Nullable
    private static String trimToNull(@Nullable String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static BusinessException error(String code, String message) {
        return new BusinessException(code, message);
    }

    private BusinessException invalidPhone() {
        return error(Errors.INVALID_PHONE, MSG_INVALID_PHONE);
    }

    private BusinessException notRegistered() {
        return error(Errors.NOT_REGISTERED, MSG_NOT_REGISTERED);
    }

    private BusinessException codeInvalid() {
        return error(Errors.CODE_INVALID, MSG_CODE_INVALID);
    }

    private BusinessException weakPassword() {
        return error(Errors.WEAK_PASSWORD, MSG_WEAK_PASSWORD);
    }

    /** 剩余锁定时间向上取整到分钟，至少 1 分钟 */
    private static BusinessException locked(long remainingMillis) {
        long minutes = Math.max(1, (remainingMillis + 59_999) / 60_000);
        return error(Errors.LOCKED, "密码错误次数过多，请 " + minutes + " 分钟后再试");
    }
}
