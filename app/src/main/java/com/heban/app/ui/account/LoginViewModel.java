package com.heban.app.ui.account;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.R;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;
import com.heban.app.util.PhoneMask;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * A03 密码登录（技术准备文档 12.1）。
 * 「登录」在手机号满 11 位且密码不为空时可点；未勾选协议时提示「请先阅读并勾选同意协议」；
 * 登录失败时直接显示 BusinessException 的提示（账号或密码错误还剩几次、已锁定、未注册、未设置密码）。
 */
public class LoginViewModel extends BaseViewModel {
    /** 输入框内容（可能带空格，如 138 0000 5678） */
    public final MutableLiveData<String> phone = new MutableLiveData<>("");
    public final MutableLiveData<String> password = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> agreed = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 手机号 11 位且密码不为空 */
    public final MediatorLiveData<Boolean> canSubmit = new MediatorLiveData<>();
    /** 协议下方的红字提示：点登录时未勾选显示，勾选后隐藏 */
    public final MediatorLiveData<Boolean> showAgreementHint = new MediatorLiveData<>();

    /** 一次性提示（字符串资源） */
    public final SingleLiveEvent<Integer> messageRes = new SingleLiveEvent<>();
    /** 登录成功或游客浏览：进入首页 */
    public final SingleLiveEvent<Boolean> goHome = new SingleLiveEvent<>();

    private final UserRepository repo;
    private boolean leaving;

    public LoginViewModel(AppContainer c) {
        this(c, c.userRepository);
    }

    @VisibleForTesting
    LoginViewModel(AppContainer c, UserRepository repo) {
        super(c);
        this.repo = repo;
        canSubmit.setValue(false);
        canSubmit.addSource(phone, v -> canSubmit.setValue(isFormComplete()));
        canSubmit.addSource(password, v -> canSubmit.setValue(isFormComplete()));
        showAgreementHint.setValue(false);
        showAgreementHint.addSource(agreed, a -> {
            if (Boolean.TRUE.equals(a)) showAgreementHint.setValue(false);
        });
    }

    boolean isFormComplete() {
        String pwd = password.getValue();
        return LoginPhoneInput.digits(phone.getValue()).length() == LoginPhoneInput.PHONE_LENGTH
                && pwd != null && !pwd.isEmpty();
    }

    /** 从验证码登录页带回的手机号和勾选状态 */
    public void applyFromOtherTab(String phoneDigits, boolean agreedValue) {
        if (phoneDigits != null) phone.setValue(LoginPhoneInput.format(phoneDigits));
        agreed.setValue(agreedValue);
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
        add(repo.loginWithPassword(p, password.getValue())
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
