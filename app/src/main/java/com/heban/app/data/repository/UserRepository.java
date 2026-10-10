package com.heban.app.data.repository;

import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.remote.sms.SmsScene;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 账号相关数据（技术准备文档表 5-2）。mock：账号存 Room，短信走 SmsService；remote：调用后端接口。
 * 所有方法在内部切换到 io 线程；业务错误以 BusinessException 抛出，message 可直接显示。
 * BusinessException.code 取值见 {@link Errors}。
 */
public interface UserRepository {

    final class Errors {
        public static final String NOT_REGISTERED = "NOT_REGISTERED";   // 该手机号未注册
        public static final String WRONG_PASSWORD = "WRONG_PASSWORD";   // 账号或密码错误（附剩余次数）
        public static final String LOCKED = "LOCKED";                   // 连续输错 5 次，锁定 10 分钟
        public static final String NO_PASSWORD = "NO_PASSWORD";         // 账号未设置密码，请用验证码登录
        public static final String CODE_INVALID = "CODE_INVALID";       // 验证码错误或已过期
        public static final String PHONE_EXISTS = "PHONE_EXISTS";       // 手机号已注册
        public static final String SAME_AS_RECENT = "SAME_AS_RECENT";   // 新密码与最近使用过的密码相同
        public static final String SAME_PHONE = "SAME_PHONE";           // 新手机号与原手机号相同
        public static final String INVALID_PHONE = "INVALID_PHONE";     // 请输入正确的手机号
        public static final String WEAK_PASSWORD = "WEAK_PASSWORD";     // 密码需为 8–20 位，并同时包含字母和数字

        private Errors() {
        }
    }

    /** 发送验证码（A04、A05、A07、A09） */
    Completable sendCode(String phone, SmsScene scene);

    /** 校验验证码（A05 第 1 步、A07、A09 第 2 步）；错误时抛出 CODE_INVALID */
    Completable verifyCode(String phone, String code);

    Single<Boolean> isRegistered(String phone);

    /** A03 密码登录；成功后写入登录态 */
    Single<UserEntity> loginWithPassword(String phone, String password);

    /** A04 验证码登录；手机号未注册时自动创建账号，成功后写入登录态 */
    Single<UserEntity> loginWithSms(String phone, String code);

    /** A05 + A06 注册；成功后写入登录态 */
    Single<UserEntity> register(RegisterForm form);

    /** A08 重置密码；成功后清除登录态（其他设备上的登录失效） */
    Completable resetPassword(String phone, String newPassword);

    /** A09 提交申诉，返回申诉编号（如 HB20261010-0832） */
    Single<String> submitAppeal(AppealEntity appeal);

    /** 游客浏览：写入 session.guest */
    Completable enterGuest();

    /** 引导页看完或跳过：写入 onboarding.done */
    Completable completeOnboarding();

    /** 退出登录：清除登录态，保留本机作物数据 */
    Completable logout();

    /** 当前登录用户；游客或未登录时不发射 */
    Flowable<UserEntity> currentUser();

    /** 短信是否为模拟模式（界面在发送成功后提示「模拟短信：123456」） */
    boolean isFakeSms();
}
