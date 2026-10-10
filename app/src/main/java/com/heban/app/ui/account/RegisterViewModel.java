package com.heban.app.ui.account;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.heban.app.R;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.sms.FakeSmsService;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.RegisterForm;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;
import com.heban.app.ui.common.SmsCountdown;
import com.heban.app.util.PasswordStrength;
import com.heban.app.util.PhoneMask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * A05 注册 + A06 完善资料 共享的 ViewModel（作用域为 register_graph 返回栈条目，技术准备文档表 6-1）。
 * A05：手机号 + 验证码 + 密码（8–20 位、同时包含字母和数字）；A06：昵称、地区、种植身份、主要作物，可跳过。
 */
public class RegisterViewModel extends BaseViewModel {
    /** 种植身份（单选，默认第一项） */
    public static final List<String> IDENTITIES =
            Collections.unmodifiableList(Arrays.asList("种植户", "家庭园艺", "农技人员"));
    /** 主要作物（多选） */
    public static final List<String> CROP_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            "番茄", "黄瓜", "草莓", "小麦", "水稻", "玉米", "果树", "茶叶", "食用菌", "中药材"));
    /** 原型中的默认选中项 */
    static final List<String> DEFAULT_CROPS = Collections.unmodifiableList(Arrays.asList("番茄", "黄瓜", "小麦"));
    public static final int NICKNAME_MAX = 16;

    private final UserRepository repo;

    // ---------- A05 注册 ----------
    /** 手机号输入框内容，可能含有 3-4-4 分隔空格 */
    public final MutableLiveData<String> phone = new MutableLiveData<>("");
    public final MutableLiveData<String> code = new MutableLiveData<>("");
    public final MutableLiveData<String> password = new MutableLiveData<>("");
    public final MutableLiveData<String> confirmPassword = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> agreed = new MutableLiveData<>(false);
    public final SmsCountdown countdown = new SmsCountdown();

    public final LiveData<PasswordStrength.Result> strength =
            Transformations.map(password, PasswordStrength::evaluate);
    /** 两次密码都已输入且一致（确认密码框显示对勾） */
    public final LiveData<Boolean> passwordsMatch;
    /** 确认密码与密码不一致（确认密码框下方显示错误） */
    public final LiveData<Boolean> confirmMismatch;
    /** 「下一步」是否可点击（协议是否勾选在点击时检查；提交中由 next() 忽略重复点击） */
    public final LiveData<Boolean> canNext;

    // ---------- A06 完善资料 ----------
    public final MutableLiveData<String> nickname = new MutableLiveData<>("");
    public final LiveData<Integer> nicknameLength;
    /** 头像中显示的昵称首字；昵称为空时为 null（界面显示「农」） */
    public final LiveData<String> avatarInitial;
    /** 本次选择的头像（content:// 地址）；M1 只在本次会话中显示，不随注册保存 */
    public final MutableLiveData<String> avatarUri = new MutableLiveData<>(null);
    public final MutableLiveData<String> region = new MutableLiveData<>("");
    public final MutableLiveData<String> identity = new MutableLiveData<>(IDENTITIES.get(0));
    public final MutableLiveData<Set<String>> selectedCrops =
            new MutableLiveData<>(new LinkedHashSet<>(DEFAULT_CROPS));
    public final LiveData<Integer> cropCount;

    // ---------- 公共 ----------
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 提示文字资源（Snackbar） */
    public final SingleLiveEvent<Integer> toastRes = new SingleLiveEvent<>();
    /** 模拟短信发送成功，值为验证码 */
    public final SingleLiveEvent<String> fakeSmsSent = new SingleLiveEvent<>();
    /** A05 验证通过 → A06 */
    public final SingleLiveEvent<Boolean> toProfileSetup = new SingleLiveEvent<>();
    /** 注册成功 → 首页 */
    public final SingleLiveEvent<Boolean> enterHome = new SingleLiveEvent<>();

    private boolean sending;

    public RegisterViewModel(AppContainer c) {
        this(c, c.userRepository);
    }

    @VisibleForTesting
    RegisterViewModel(@Nullable AppContainer c, @NonNull UserRepository repo) {
        super(c);
        this.repo = repo;

        MediatorLiveData<Boolean> match = new MediatorLiveData<>();
        match.addSource(password, v -> match.setValue(computePasswordsMatch()));
        match.addSource(confirmPassword, v -> match.setValue(computePasswordsMatch()));
        passwordsMatch = match;

        MediatorLiveData<Boolean> mismatch = new MediatorLiveData<>();
        mismatch.addSource(password, v -> mismatch.setValue(computeConfirmMismatch()));
        mismatch.addSource(confirmPassword, v -> mismatch.setValue(computeConfirmMismatch()));
        confirmMismatch = mismatch;

        MediatorLiveData<Boolean> next = new MediatorLiveData<>();
        next.addSource(phone, v -> next.setValue(computeCanNext()));
        next.addSource(code, v -> next.setValue(computeCanNext()));
        next.addSource(password, v -> next.setValue(computeCanNext()));
        next.addSource(confirmPassword, v -> next.setValue(computeCanNext()));
        canNext = next;

        nicknameLength = Transformations.map(nickname, s -> s == null ? 0 : s.length());
        avatarInitial = Transformations.map(nickname, RegisterViewModel::initialOf);
        cropCount = Transformations.map(selectedCrops, s -> s == null ? 0 : s.size());
    }

    // ---------- A05 ----------

    /** 手机号去掉分隔空格后的 11 位数字 */
    String phoneDigits() {
        String p = phone.getValue();
        return p == null ? "" : p.replaceAll("\\D", "");
    }

    boolean computePasswordsMatch() {
        String p = text(password);
        return !p.isEmpty() && p.equals(text(confirmPassword));
    }

    /**
     * 确认密码已输入、与密码不同，且已经不可能再「输完」变成一致（长度已够或前缀已不同）时提示不一致，
     * 避免用户逐字输入时一直显示错误。
     */
    boolean computeConfirmMismatch() {
        String p = text(password);
        String c = text(confirmPassword);
        if (c.isEmpty() || c.equals(p)) return false;
        return c.length() >= p.length() || !p.startsWith(c);
    }

    boolean computeCanNext() {
        return PhoneMask.isValid(phoneDigits())
                && !text(code).trim().isEmpty()
                && PasswordStrength.evaluate(text(password)).valid()
                && computePasswordsMatch();
    }

    /** 获取验证码（倒计时进行中时忽略） */
    public void sendCode() {
        if (sending || !Boolean.TRUE.equals(countdown.canSend.getValue())) return;
        String p = phoneDigits();
        if (!PhoneMask.isValid(p)) {
            toastRes.setValue(R.string.register_phone_invalid);
            return;
        }
        sending = true;
        add(Completable.defer(() -> repo.sendCode(p, SmsScene.REGISTER))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    sending = false;
                    countdown.start(disposables);
                    if (repo.isFakeSms()) fakeSmsSent.setValue(FakeSmsService.CODE);
                }, e -> {
                    sending = false;
                    toast.setValue(AppError.from(e).message);
                }));
    }

    /** 下一步：校验验证码后进入 A06 */
    public void next() {
        if (isLoading() || !computeCanNext()) return;
        if (!Boolean.TRUE.equals(agreed.getValue())) {
            toastRes.setValue(R.string.agreement_required);
            return;
        }
        loading.setValue(true);
        String p = phoneDigits();
        String c = text(code).trim();
        add(Completable.defer(() -> repo.verifyCode(p, c))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    loading.setValue(false);
                    toProfileSetup.setValue(true);
                }, e -> {
                    loading.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    // ---------- A06 ----------

    public void selectIdentity(String name) {
        if (IDENTITIES.contains(name) && !name.equals(identity.getValue())) identity.setValue(name);
    }

    public void setCropSelected(String name, boolean selected) {
        Set<String> cur = selectedCrops.getValue();
        if (cur == null) cur = new LinkedHashSet<>();
        if (cur.contains(name) == selected || !CROP_OPTIONS.contains(name)) return;
        Set<String> next = new LinkedHashSet<>(cur);
        if (selected) next.add(name);
        else next.remove(name);
        selectedCrops.setValue(next);
    }

    public boolean isCropSelected(String name) {
        Set<String> cur = selectedCrops.getValue();
        return cur != null && cur.contains(name);
    }

    /** 进入禾伴：带上填写的资料注册 */
    public void enterApp() {
        submit(buildForm(false));
    }

    /** 跳过：只用手机号和密码注册，昵称由仓库生成默认值 */
    public void skip() {
        submit(buildForm(true));
    }

    RegisterForm buildForm(boolean skipped) {
        RegisterForm f = new RegisterForm();
        f.phone = phoneDigits();
        f.password = text(password);
        if (skipped) {
            f.nickname = null;
            f.region = null;
            f.identity = null;
            f.crops = new ArrayList<>();
            return f;
        }
        f.nickname = emptyToNull(text(nickname).trim());
        f.region = emptyToNull(text(region).trim().replaceAll("\\s+", " "));
        f.identity = identity.getValue();
        List<String> crops = new ArrayList<>();
        for (String name : CROP_OPTIONS) {
            if (isCropSelected(name)) crops.add(name);
        }
        f.crops = crops;
        return f;
    }

    private void submit(RegisterForm form) {
        if (isLoading()) return;
        loading.setValue(true);
        add(Single.defer(() -> repo.register(form))
                .observeOn(AndroidSchedulers.mainThread())
                // 成功后保持 loading，避免跳转过程中再次提交
                .subscribe(user -> enterHome.setValue(true), e -> {
                    loading.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    // ---------- 工具 ----------

    public boolean isLoading() {
        return Boolean.TRUE.equals(loading.getValue());
    }

    @Nullable
    static String initialOf(@Nullable String nickname) {
        if (nickname == null) return null;
        String t = nickname.trim();
        if (t.isEmpty()) return null;
        return new String(Character.toChars(t.codePointAt(0)));
    }

    private static String text(LiveData<String> data) {
        String v = data.getValue();
        return v == null ? "" : v;
    }

    @Nullable
    private static String emptyToNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
