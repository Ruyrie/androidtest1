package com.heban.app.ui.account;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.R;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;
import com.heban.app.ui.common.SmsCountdown;
import com.heban.app.util.PhoneMask;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * A04 验证码登录（技术准备文档 12.1）：未注册的手机号验证通过后自动创建账号。
 * 「获取验证码」发送成功后倒计时 60 秒；模拟短信模式下提示固定验证码。
 */
public class LoginSmsViewModel extends BaseViewModel {
    public static final int CODE_LENGTH = 6;

    /** 输入框内容（可能带空格，如 138 0000 5678） */
    public final MutableLiveData<String> phone = new MutableLiveData<>("");
    public final MutableLiveData<String> code = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> agreed = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public final SmsCountdown countdown = new SmsCountdown();
    /** 验证码发送到的号码（脱敏，如 138****5678），发送成功前为 null */
    public final MutableLiveData<String> sentTo = new MutableLiveData<>(null);

    /** 不在倒计时中且没有正在发送的请求 */
    public final MediatorLiveData<Boolean> canSendCode = new MediatorLiveData<>();
    /** 「验证码已发送至 …，5 分钟内有效」：倒计时期间显示 */
    public final MediatorLiveData<Boolean> codeSentVisible = new MediatorLiveData<>();
    /** 手机号 11 位且验证码 6 位 */
    public final MediatorLiveData<Boolean> canSubmit = new MediatorLiveData<>();
    /** 协议下方的红字提示：点登录时未勾选显示，勾选后隐藏 */
    public final MediatorLiveData<Boolean> showAgreementHint = new MediatorLiveData<>();

    /** 一次性提示（字符串资源） */
    public final SingleLiveEvent<Integer> messageRes = new SingleLiveEvent<>();
    /** 模拟短信发送成功：界面提示「模拟短信：验证码 123456」 */
    public final SingleLiveEvent<Boolean> fakeSmsSent = new SingleLiveEvent<>();
    /** 登录成功或游客浏览：进入首页 */
    public final SingleLiveEvent<Boolean> goHome = new SingleLiveEvent<>();

    private final UserRepository repo;
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    private boolean leaving;
    private boolean prefilled;

    public LoginSmsViewModel(AppContainer c) {
        this(c, c.userRepository);
    }

    @VisibleForTesting
    LoginSmsViewModel(AppContainer c, UserRepository repo) {
        super(c);
        this.repo = repo;
        canSendCode.setValue(true);
        canSendCode.addSource(countdown.canSend, v -> canSendCode.setValue(isSendAllowed()));
        canSendCode.addSource(sending, v -> canSendCode.setValue(isSendAllowed()));
        codeSentVisible.setValue(false);
        codeSentVisible.addSource(countdown.secondsLeft, v -> codeSentVisible.setValue(isCodeSentVisible()));
        codeSentVisible.addSource(sentTo, v -> codeSentVisible.setValue(isCodeSentVisible()));
        canSubmit.setValue(false);
        canSubmit.addSource(phone, v -> canSubmit.setValue(isFormComplete()));
        canSubmit.addSource(code, v -> canSubmit.setValue(isFormComplete()));
        showAgreementHint.setValue(false);
        showAgreementHint.addSource(agreed, a -> {
            if (Boolean.TRUE.equals(a)) showAgreementHint.setValue(false);
        });
    }

    private boolean isSendAllowed() {
        return Boolean.TRUE.equals(countdown.canSend.getValue()) && !Boolean.TRUE.equals(sending.getValue());
    }

    private boolean isCodeSentVisible() {
        return sentTo.getValue() != null && countdown.isRunning();
    }

    boolean isFormComplete() {
        String c = code.getValue();
        return LoginPhoneInput.digits(phone.getValue()).length() == LoginPhoneInput.PHONE_LENGTH
                && c != null && c.length() == CODE_LENGTH;
    }

    /** 从密码登录页带过来的手机号和勾选状态（每个 ViewModel 只应用一次，返回本页时不覆盖用户的修改） */
    public void prefillOnce(String phoneDigits, boolean agreedValue) {
        if (prefilled) return;
        prefilled = true;
        if (phoneDigits != null) phone.setValue(LoginPhoneInput.format(phoneDigits));
        agreed.setValue(agreedValue);
    }

    public void sendCode() {
        if (!isSendAllowed()) return;
        String p = LoginPhoneInput.digits(phone.getValue());
        if (!PhoneMask.isValid(p)) {
            messageRes.setValue(R.string.login_phone_invalid);
            return;
        }
        sending.setValue(true);
        add(repo.sendCode(p, SmsScene.LOGIN)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    sentTo.setValue(PhoneMask.mask(p));
                    countdown.start(disposables);
                    sending.setValue(false);
                    if (repo.isFakeSms()) fakeSmsSent.setValue(true);
                }, e -> {
                    sending.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    public void login() {
        if (Boolean.TRUE.equals(loading.getValue()) || leaving || !isFormComplete()) return;
        if (!Boolean.TRUE.equals(agreed.getValue())) {
            showAgreementHint.setValue(true);
            messageRes.setValue(R.string.agreement_required);
            return;
        }
        String p = LoginPhoneInput.digits(phone.getValue());
        if (!PhoneMask.isValid(p)) {
            messageRes.setValue(R.string.login_phone_invalid);
            return;
        }
        loading.setValue(true);
        add(repo.loginWithSms(p, code.getValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    leaving = true;
                    loading.setValue(false);
                    goHome.setValue(true);
                }, e -> {
                    loading.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    public void enterGuest() {
        if (Boolean.TRUE.equals(loading.getValue()) || leaving) return;
        leaving = true;
        add(repo.enterGuest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> goHome.setValue(true), e -> {
                    leaving = false;
                    toast.setValue(AppError.from(e).message);
                }));
    }
}
