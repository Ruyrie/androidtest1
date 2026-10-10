package com.heban.app.ui.account;

import static com.heban.app.ui.account.ResetPwdViewModel.derived;
import static com.heban.app.ui.account.ResetPwdViewModel.digits;
import static com.heban.app.ui.account.ResetPwdViewModel.isCode;
import static com.heban.app.ui.account.ResetPwdViewModel.isTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;
import com.heban.app.ui.common.SmsCountdown;
import com.heban.app.util.PhoneMask;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;

/**
 * A09 手机号已停用 · 申诉换绑：同一页面内三步。
 * 1 账号信息（原手机号、昵称、最近一次登录、常用登录地区）→ 2 验证新手机号（CHANGE_PHONE 验证码）→ 3 已提交（人工审核中）。
 */
public class PhoneAppealViewModel extends BaseViewModel {
    public static final int STEP_ACCOUNT = 1;
    public static final int STEP_NEW_PHONE = 2;
    public static final int STEP_SUBMITTED = 3;

    public static final String DEFAULT_LAST_LOGIN = "一个月内";
    public static final String DEFAULT_REGION = "江苏 南京";

    private final UserRepository repo;

    public final MutableLiveData<Integer> step = new MutableLiveData<>(STEP_ACCOUNT);

    // ---------- 第 1 步 账号信息 ----------
    public final MutableLiveData<String> oldPhone = new MutableLiveData<>("");
    public final MutableLiveData<String> nickname = new MutableLiveData<>("");
    public final MutableLiveData<String> lastLogin = new MutableLiveData<>(DEFAULT_LAST_LOGIN);
    public final MutableLiveData<String> region = new MutableLiveData<>(DEFAULT_REGION);

    public final MediatorLiveData<Boolean> canNext = derived(
            () -> PhoneMask.isValid(digits(oldPhone.getValue())), oldPhone);

    // ---------- 第 2 步 验证新手机号 ----------
    public final MutableLiveData<String> newPhone = new MutableLiveData<>("");
    public final MutableLiveData<String> code = new MutableLiveData<>("");
    public final SmsCountdown countdown = new SmsCountdown();
    public final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    /** 验证码已发送到的手机号（脱敏），未发送时为 null */
    public final MutableLiveData<String> sentMasked = new MutableLiveData<>(null);
    public final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    public final SingleLiveEvent<Boolean> fakeSmsSent = new SingleLiveEvent<>();

    /** 新手机号与原手机号相同 */
    public final MediatorLiveData<Boolean> samePhone = derived(() -> {
        String n = digits(newPhone.getValue());
        return !n.isEmpty() && n.equals(digits(oldPhone.getValue()));
    }, newPhone, oldPhone);

    public final MediatorLiveData<Boolean> canSendCode = derived(
            () -> newPhoneOk() && isTrue(countdown.canSend) && !isTrue(sending),
            newPhone, oldPhone, countdown.canSend, sending);

    public final MediatorLiveData<Boolean> canSubmit = derived(
            () -> newPhoneOk() && isCode(code.getValue()),
            newPhone, oldPhone, code);

    // ---------- 第 3 步 已提交 ----------
    public final MutableLiveData<String> appealNo = new MutableLiveData<>("");
    public final MutableLiveData<String> submittedOldMasked = new MutableLiveData<>("");
    public final MutableLiveData<String> submittedNewMasked = new MutableLiveData<>("");

    public PhoneAppealViewModel(AppContainer c) {
        this(c, c.userRepository);
    }

    /** 单元测试注入假的 UserRepository */
    PhoneAppealViewModel(@Nullable AppContainer c, @NonNull UserRepository repo) {
        super(c);
        this.repo = repo;
    }

    private boolean newPhoneOk() {
        String n = digits(newPhone.getValue());
        return PhoneMask.isValid(n) && !n.equals(digits(oldPhone.getValue()));
    }

    /** 第 1 步「下一步」 */
    public void next() {
        if (!isStep(STEP_ACCOUNT) || !isTrue(canNext)) return;
        step.setValue(STEP_NEW_PHONE);
    }

    /** 返回键 / 顶栏返回：第 2 步回到第 1 步；返回 false 表示交给系统处理 */
    public boolean back() {
        if (isStep(STEP_NEW_PHONE) && !isTrue(submitting)) {
            step.setValue(STEP_ACCOUNT);
            return true;
        }
        return false;
    }

    /** 第 2 步「获取验证码」 */
    public void sendCode() {
        if (!isTrue(canSendCode)) return;
        String p = digits(newPhone.getValue());
        sending.setValue(true);
        add(repo.sendCode(p, SmsScene.CHANGE_PHONE)
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

    /** 第 2 步「提交申诉」：校验新手机号验证码 → 保存申诉记录 → 第 3 步 */
    public void submit() {
        if (!isStep(STEP_NEW_PHONE) || isTrue(submitting) || !isTrue(canSubmit)) return;
        AppealEntity appeal = buildAppeal();
        submitting.setValue(true);
        add(repo.verifyCode(appeal.newPhone, code.getValue())
                .andThen(Single.defer(() -> repo.submitAppeal(appeal)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(no -> {
                    submitting.setValue(false);
                    countdown.cancel();
                    appealNo.setValue(no);
                    submittedOldMasked.setValue(PhoneMask.mask(appeal.oldPhone));
                    submittedNewMasked.setValue(PhoneMask.mask(appeal.newPhone));
                    step.setValue(STEP_SUBMITTED);
                }, e -> {
                    submitting.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    AppealEntity buildAppeal() {
        AppealEntity a = new AppealEntity();
        a.oldPhone = digits(oldPhone.getValue());
        a.newPhone = digits(newPhone.getValue());
        String nick = nickname.getValue() == null ? "" : nickname.getValue().trim();
        a.nickname = nick.isEmpty() ? null : nick;
        a.lastLogin = lastLogin.getValue();
        String r = region.getValue() == null ? "" : region.getValue().trim();
        a.region = r.isEmpty() ? null : r;
        a.status = AppealEntity.STATUS_PENDING;
        a.createdAt = System.currentTimeMillis();
        return a;
    }

    private boolean isStep(int s) {
        Integer cur = step.getValue();
        return cur != null && cur == s;
    }

    @Override
    protected void onCleared() {
        countdown.cancel();
        super.onCleared();
    }
}
