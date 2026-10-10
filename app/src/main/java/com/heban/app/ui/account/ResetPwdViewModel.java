package com.heban.app.ui.account;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;
import com.heban.app.ui.common.SmsCountdown;
import com.heban.app.util.PasswordStrength;
import com.heban.app.util.PhoneMask;

import java.util.Objects;
import java.util.function.Supplier;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * A07 验证手机 + A08 设置新密码，两步共享（作用域为 reset_graph 返回栈条目）。
 * 流程：发送验证码（RESET_PASSWORD）→ 校验验证码 → 重置密码 → 「密码已重置」。
 */
public class ResetPwdViewModel extends BaseViewModel {

    /** 密码规则的显示状态 */
    public enum Rule {
        /** 未满足（或尚未输入） */
        UNMET,
        /** 已满足 */
        MET,
        /** 提交后才能知道（与最近使用过的密码不同） */
        PENDING,
        /** 提交后被拒绝 */
        FAILED
    }

    /** 确认新密码的状态 */
    public enum Confirm { EMPTY, MATCH, MISMATCH }

    private final UserRepository repo;

    // ---------- A07 验证手机 ----------
    /** 手机号输入框内容（可能带 3-4-4 空格） */
    public final MutableLiveData<String> phone = new MutableLiveData<>("");
    public final MutableLiveData<String> code = new MutableLiveData<>("");
    public final SmsCountdown countdown = new SmsCountdown();
    /** 正在请求发送验证码 */
    public final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    /** 验证码发送成功后的脱敏手机号（138****5678），未发送时为 null */
    public final MutableLiveData<String> sentMasked = new MutableLiveData<>(null);
    /** 正在校验验证码 */
    public final MutableLiveData<Boolean> verifying = new MutableLiveData<>(false);
    /** 模拟短信模式下发送成功：界面提示固定验证码 */
    public final SingleLiveEvent<Boolean> fakeSmsSent = new SingleLiveEvent<>();
    /** 验证码校验通过：进入 A08 */
    public final SingleLiveEvent<Boolean> toResetPwd = new SingleLiveEvent<>();

    public final MediatorLiveData<Boolean> canSendCode = derived(
            () -> PhoneMask.isValid(digits(phone.getValue()))
                    && isTrue(countdown.canSend) && !isTrue(sending),
            phone, countdown.canSend, sending);

    public final MediatorLiveData<Boolean> canNext = derived(
            () -> PhoneMask.isValid(digits(phone.getValue())) && isCode(code.getValue()),
            phone, code);

    // ---------- A08 设置新密码 ----------
    public final MutableLiveData<String> newPwd = new MutableLiveData<>("");
    public final MutableLiveData<String> confirmPwd = new MutableLiveData<>("");
    /** 提交后被判定为「与最近使用过的密码相同」的那个密码；改动新密码后规则恢复为待校验 */
    private final MutableLiveData<String> rejectedPwd = new MutableLiveData<>(null);
    public final MutableLiveData<Boolean> resetting = new MutableLiveData<>(false);
    /** 重置成功：显示「密码已重置」 */
    public final MutableLiveData<Boolean> done = new MutableLiveData<>(false);

    public final MediatorLiveData<PasswordStrength.Result> strength = derived(
            () -> PasswordStrength.evaluate(newPwd.getValue()), newPwd);

    public final MediatorLiveData<Rule> lengthRule = derived(
            () -> PasswordStrength.evaluate(newPwd.getValue()).lengthOk ? Rule.MET : Rule.UNMET, newPwd);

    public final MediatorLiveData<Rule> mixRule = derived(() -> {
        PasswordStrength.Result r = PasswordStrength.evaluate(newPwd.getValue());
        return r.hasLetter && r.hasDigit ? Rule.MET : Rule.UNMET;
    }, newPwd);

    public final MediatorLiveData<Rule> recentRule = derived(() -> {
        String rejected = rejectedPwd.getValue();
        return rejected != null && rejected.equals(newPwd.getValue()) ? Rule.FAILED : Rule.PENDING;
    }, newPwd, rejectedPwd);

    public final MediatorLiveData<Confirm> confirm = derived(() -> {
        String c = confirmPwd.getValue();
        if (c == null || c.isEmpty()) return Confirm.EMPTY;
        return c.equals(newPwd.getValue()) ? Confirm.MATCH : Confirm.MISMATCH;
    }, newPwd, confirmPwd);

    public final MediatorLiveData<Boolean> canFinish = derived(() -> {
        String pwd = newPwd.getValue();
        String rejected = rejectedPwd.getValue();
        return PasswordStrength.evaluate(pwd).valid()
                && Objects.equals(pwd, confirmPwd.getValue())
                && !(rejected != null && rejected.equals(pwd));
    }, newPwd, confirmPwd, rejectedPwd);

    /** 验证通过的手机号（A08 重置时使用） */
    @Nullable
    private String verifiedPhone;

    public ResetPwdViewModel(AppContainer c) {
        this(c, c.userRepository);
    }

    /** 单元测试注入假的 UserRepository */
    ResetPwdViewModel(@Nullable AppContainer c, @NonNull UserRepository repo) {
        super(c);
        this.repo = repo;
    }

    /** A07「获取验证码」 */
    public void sendCode() {
        if (!isTrue(canSendCode)) return;
        String p = digits(phone.getValue());
        sending.setValue(true);
        add(repo.sendCode(p, SmsScene.RESET_PASSWORD)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    sending.setValue(false);
                    sentMasked.setValue(PhoneMask.mask(p));
                    countdown.start(disposables);
                    if (repo.isFakeSms()) fakeSmsSent.setValue(true);
                }, e -> {
                    sending.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    /** A07「下一步」：校验验证码后进入 A08 */
    public void next() {
        if (isTrue(verifying) || !isTrue(canNext)) return;
        String p = digits(phone.getValue());
        verifying.setValue(true);
        add(repo.verifyCode(p, code.getValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    verifying.setValue(false);
                    verifiedPhone = p;
                    toResetPwd.setValue(true);
                }, e -> {
                    verifying.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    /** A08「完成」：重置密码（成功后其他设备的登录失效） */
    public void finish() {
        if (isTrue(resetting) || !isTrue(canFinish)) return;
        String p = verifiedPhone != null ? verifiedPhone : digits(phone.getValue());
        String pwd = newPwd.getValue();
        resetting.setValue(true);
        add(repo.resetPassword(p, pwd)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    resetting.setValue(false);
                    countdown.cancel();
                    done.setValue(true);
                }, e -> {
                    resetting.setValue(false);
                    if (e instanceof BusinessException
                            && UserRepository.Errors.SAME_AS_RECENT.equals(((BusinessException) e).code)) {
                        rejectedPwd.setValue(pwd);
                    }
                    toast.setValue(AppError.from(e).message);
                }));
    }

    /** 强度条点亮的格数：未输入 0、弱 1、中 2、强 3 */
    public static int strengthSegments(@Nullable PasswordStrength.Result r) {
        if (r == null) return 0;
        switch (r.level) {
            case WEAK: return 1;
            case MEDIUM: return 2;
            case STRONG: return 3;
            default: return 0;
        }
    }

    @Nullable
    String verifiedPhone() {
        return verifiedPhone;
    }

    @Override
    protected void onCleared() {
        countdown.cancel();
        super.onCleared();
    }

    // ---------- 工具 ----------

    /** 去掉空格等非数字字符 */
    static String digits(@Nullable String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    static boolean isCode(@Nullable String s) {
        return s != null && s.matches("\\d{6}");
    }

    static boolean isTrue(LiveData<Boolean> b) {
        return Boolean.TRUE.equals(b.getValue());
    }

    /** 由若干输入派生出的状态：任一输入变化时重新计算 */
    static <T> MediatorLiveData<T> derived(Supplier<T> fn, LiveData<?>... sources) {
        MediatorLiveData<T> m = new MediatorLiveData<>(fn.get());
        for (LiveData<?> s : sources) {
            m.addSource(s, x -> {
                T v = fn.get();
                if (!Objects.equals(v, m.getValue())) m.setValue(v);
            });
        }
        return m;
    }
}
