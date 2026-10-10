package com.heban.app.ui.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.heban.app.R;
import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.RegisterForm;
import com.heban.app.data.repository.UserRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;

public class RegisterViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    private final TestScheduler timer = new TestScheduler();
    private FakeUserRepository repo;
    private RegisterViewModel vm;

    @Before
    public void setUp() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(s -> timer);
        repo = new FakeUserRepository();
        vm = new RegisterViewModel(null, repo);
        observe(vm.canNext);
        observe(vm.passwordsMatch);
        observe(vm.confirmMismatch);
        observe(vm.cropCount);
        observe(vm.nicknameLength);
        observe(vm.avatarInitial);
        observe(vm.strength);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private static <T> void observe(LiveData<T> data) {
        data.observeForever(v -> { });
    }

    private void fillStep1() {
        vm.phone.setValue("138 0000 5678");
        vm.code.setValue("482915");
        vm.password.setValue("heban2026");
        vm.confirmPassword.setValue("heban2026");
    }

    // ---------- A05：「下一步」可用规则 ----------

    @Test
    public void canNext_falseInitially_trueWhenAllValid() {
        assertFalse(vm.canNext.getValue());
        fillStep1();
        assertTrue(vm.canNext.getValue());
        assertEquals("13800005678", vm.phoneDigits());
    }

    @Test
    public void canNext_falseWhenPhoneInvalid() {
        fillStep1();
        vm.phone.setValue("128 0000 5678");
        assertFalse(vm.canNext.getValue());
        vm.phone.setValue("138 0000 567");
        assertFalse(vm.canNext.getValue());
    }

    @Test
    public void canNext_falseWhenCodeEmpty() {
        fillStep1();
        vm.code.setValue("  ");
        assertFalse(vm.canNext.getValue());
    }

    @Test
    public void canNext_falseWhenPasswordRulesNotMet() {
        fillStep1();
        vm.password.setValue("abcdefgh");          // 没有数字
        vm.confirmPassword.setValue("abcdefgh");
        assertFalse(vm.canNext.getValue());
        vm.password.setValue("12345678");          // 没有字母
        vm.confirmPassword.setValue("12345678");
        assertFalse(vm.canNext.getValue());
        vm.password.setValue("ab12");              // 太短
        vm.confirmPassword.setValue("ab12");
        assertFalse(vm.canNext.getValue());
        vm.password.setValue("abcdefghij1234567890x");   // 超过 20 位
        vm.confirmPassword.setValue("abcdefghij1234567890x");
        assertFalse(vm.canNext.getValue());
    }

    @Test
    public void canNext_falseWhenPasswordsDiffer() {
        fillStep1();
        vm.confirmPassword.setValue("heban2027");
        assertFalse(vm.canNext.getValue());
    }

    @Test
    public void strength_followsPassword() {
        vm.password.setValue("heban2026");
        assertEquals("中", vm.strength.getValue().label());
        assertTrue(vm.strength.getValue().valid());
        vm.password.setValue("abc");
        assertEquals("弱", vm.strength.getValue().label());
        assertFalse(vm.strength.getValue().lengthOk);
        assertTrue(vm.strength.getValue().hasLetter);
        assertFalse(vm.strength.getValue().hasDigit);
    }

    // ---------- A05：两次密码 ----------

    @Test
    public void passwordsMatch_onlyWhenEqualAndNotEmpty() {
        assertFalse(vm.passwordsMatch.getValue());
        vm.password.setValue("heban2026");
        assertFalse(vm.passwordsMatch.getValue());
        vm.confirmPassword.setValue("heban2026");
        assertTrue(vm.passwordsMatch.getValue());
        vm.password.setValue("heban2027");
        assertFalse(vm.passwordsMatch.getValue());
    }

    @Test
    public void confirmMismatch_hiddenWhileTypingPrefix_shownWhenDifferent() {
        vm.password.setValue("heban2026");
        vm.confirmPassword.setValue("");
        assertFalse(vm.confirmMismatch.getValue());
        vm.confirmPassword.setValue("heb");          // 还在输入，前缀一致
        assertFalse(vm.confirmMismatch.getValue());
        vm.confirmPassword.setValue("hex");          // 前缀已不同
        assertTrue(vm.confirmMismatch.getValue());
        vm.confirmPassword.setValue("heban2027");    // 长度够了但不同
        assertTrue(vm.confirmMismatch.getValue());
        vm.confirmPassword.setValue("heban2026");
        assertFalse(vm.confirmMismatch.getValue());
        vm.confirmPassword.setValue("heban20261");   // 多输了一位
        assertTrue(vm.confirmMismatch.getValue());
    }

    // ---------- A05：验证码 ----------

    @Test
    public void sendCode_invalidPhone_showsHint() {
        vm.phone.setValue("123");
        vm.sendCode();
        assertEquals(Integer.valueOf(R.string.register_phone_invalid), vm.toastRes.getValue());
        assertEquals(0, repo.sendCount);
    }

    @Test
    public void sendCode_success_startsCountdownAndShowsFakeSms() {
        vm.phone.setValue("138 0000 5678");
        vm.sendCode();
        assertEquals(1, repo.sendCount);
        assertEquals("13800005678", repo.lastSendPhone);
        assertEquals(SmsScene.REGISTER, repo.lastScene);
        assertEquals("123456", vm.fakeSmsSent.getValue());
        assertFalse(vm.countdown.canSend.getValue());
        // 倒计时中再次点击不发送
        vm.sendCode();
        assertEquals(1, repo.sendCount);
    }

    @Test
    public void sendCode_phoneExists_showsMessage() {
        repo.sendResult = Completable.error(
                new BusinessException(UserRepository.Errors.PHONE_EXISTS, "该手机号已注册，请直接登录"));
        vm.phone.setValue("13800005678");
        vm.sendCode();
        assertEquals("该手机号已注册，请直接登录", vm.toast.getValue());
        assertTrue(vm.countdown.canSend.getValue());
    }

    // ---------- A05：下一步 ----------

    @Test
    public void next_withoutAgreement_showsAgreementRequired() {
        fillStep1();
        vm.next();
        assertEquals(Integer.valueOf(R.string.agreement_required), vm.toastRes.getValue());
        assertEquals(0, repo.verifyCount);
        assertNull(vm.toProfileSetup.getValue());
    }

    @Test
    public void next_verifiesCodeThenGoesToProfileSetup() {
        fillStep1();
        vm.agreed.setValue(true);
        vm.next();
        assertEquals(1, repo.verifyCount);
        assertEquals("13800005678", repo.lastVerifyPhone);
        assertEquals("482915", repo.lastVerifyCode);
        assertEquals(Boolean.TRUE, vm.toProfileSetup.getValue());
        assertFalse(vm.isLoading());
    }

    @Test
    public void next_ignoredWhenInvalid() {
        vm.agreed.setValue(true);
        vm.phone.setValue("13800005678");
        vm.next();
        assertEquals(0, repo.verifyCount);
    }

    @Test
    public void next_noDoubleSubmitWhileVerifying() {
        repo.verifyResult = Completable.never();
        fillStep1();
        vm.agreed.setValue(true);
        vm.next();
        vm.next();
        assertEquals(1, repo.verifyCount);
        assertTrue(vm.isLoading());
    }

    @Test
    public void next_codeInvalid_showsErrorAndStays() {
        repo.verifyResult = Completable.error(
                new BusinessException(UserRepository.Errors.CODE_INVALID, "验证码错误或已过期"));
        fillStep1();
        vm.agreed.setValue(true);
        vm.next();
        assertEquals("验证码错误或已过期", vm.toast.getValue());
        assertNull(vm.toProfileSetup.getValue());
        assertFalse(vm.isLoading());
    }

    // ---------- A06：作物、昵称 ----------

    @Test
    public void crops_defaultsAndCount() {
        assertEquals(Integer.valueOf(3), vm.cropCount.getValue());
        assertTrue(vm.isCropSelected("番茄"));
        assertTrue(vm.isCropSelected("黄瓜"));
        assertTrue(vm.isCropSelected("小麦"));

        vm.setCropSelected("草莓", true);
        assertEquals(Integer.valueOf(4), vm.cropCount.getValue());
        vm.setCropSelected("草莓", true);            // 重复选择不变
        assertEquals(Integer.valueOf(4), vm.cropCount.getValue());
        vm.setCropSelected("番茄", false);
        assertEquals(Integer.valueOf(3), vm.cropCount.getValue());
        vm.setCropSelected("榴莲", true);            // 不在选项中
        assertEquals(Integer.valueOf(3), vm.cropCount.getValue());
    }

    @Test
    public void identity_defaultAndSelection() {
        assertEquals("种植户", vm.identity.getValue());
        vm.selectIdentity("农技人员");
        assertEquals("农技人员", vm.identity.getValue());
        vm.selectIdentity("未知");
        assertEquals("农技人员", vm.identity.getValue());
    }

    @Test
    public void nickname_counterAndAvatarInitial() {
        assertEquals(Integer.valueOf(0), vm.nicknameLength.getValue());
        assertNull(vm.avatarInitial.getValue());     // 界面显示默认「农」
        vm.nickname.setValue("老李种菜");
        assertEquals(Integer.valueOf(4), vm.nicknameLength.getValue());
        assertEquals("老", vm.avatarInitial.getValue());
        vm.nickname.setValue("   ");
        assertNull(vm.avatarInitial.getValue());
    }

    // ---------- A06：注册表单 ----------

    @Test
    public void enterApp_buildsFullForm() {
        fillStep1();
        vm.nickname.setValue(" 老李种菜 ");
        vm.region.setValue("江苏省  南京市 江宁区 ");
        vm.selectIdentity("家庭园艺");
        vm.setCropSelected("中药材", true);
        vm.setCropSelected("番茄", false);
        vm.setCropSelected("草莓", true);

        vm.enterApp();

        RegisterForm f = repo.lastForm;
        assertEquals(1, repo.registerCount);
        assertEquals("13800005678", f.phone);
        assertEquals("heban2026", f.password);
        assertEquals("老李种菜", f.nickname);
        assertEquals("江苏省 南京市 江宁区", f.region);
        assertEquals("家庭园艺", f.identity);
        assertEquals(Arrays.asList("黄瓜", "草莓", "小麦", "中药材"), f.crops);   // 按选项顺序
        assertEquals(Boolean.TRUE, vm.enterHome.getValue());
    }

    @Test
    public void enterApp_blankNicknameAndRegion_becomeNull() {
        fillStep1();
        vm.nickname.setValue("  ");
        vm.region.setValue("");
        vm.enterApp();
        assertNull(repo.lastForm.nickname);
        assertNull(repo.lastForm.region);
        assertEquals("种植户", repo.lastForm.identity);
        assertEquals(Arrays.asList("番茄", "黄瓜", "小麦"), repo.lastForm.crops);
    }

    @Test
    public void skip_registersWithoutProfile() {
        fillStep1();
        vm.nickname.setValue("老李种菜");
        vm.region.setValue("江苏省 南京市 江宁区");
        vm.skip();

        RegisterForm f = repo.lastForm;
        assertEquals("13800005678", f.phone);
        assertEquals("heban2026", f.password);
        assertNull(f.nickname);
        assertNull(f.region);
        assertNull(f.identity);
        assertEquals(Collections.emptyList(), f.crops);
        assertEquals(Boolean.TRUE, vm.enterHome.getValue());
    }

    @Test
    public void register_noDoubleSubmit() {
        repo.registerResult = Single.never();
        fillStep1();
        vm.enterApp();
        vm.enterApp();
        vm.skip();
        assertEquals(1, repo.registerCount);
        assertTrue(vm.isLoading());
    }

    @Test
    public void register_error_showsMessageAndAllowsRetry() {
        repo.registerResult = Single.error(
                new BusinessException(UserRepository.Errors.PHONE_EXISTS, "该手机号已注册"));
        fillStep1();
        vm.enterApp();
        assertEquals("该手机号已注册", vm.toast.getValue());
        assertFalse(vm.isLoading());
        assertNull(vm.enterHome.getValue());

        repo.registerResult = Single.just(new UserEntity());
        vm.enterApp();
        assertEquals(2, repo.registerCount);
        assertEquals(Boolean.TRUE, vm.enterHome.getValue());
    }

    @Test
    public void repositoryThrowingSynchronously_isReportedAsError() {
        repo.throwOnRegister = true;
        fillStep1();
        vm.enterApp();
        assertEquals("出了点问题，请重试", vm.toast.getValue());
        assertFalse(vm.isLoading());
    }

    /** 只记录调用的 UserRepository */
    static final class FakeUserRepository implements UserRepository {
        Completable sendResult = Completable.complete();
        Completable verifyResult = Completable.complete();
        Single<UserEntity> registerResult = Single.just(new UserEntity());
        boolean throwOnRegister;

        int sendCount;
        String lastSendPhone;
        SmsScene lastScene;
        int verifyCount;
        String lastVerifyPhone;
        String lastVerifyCode;
        int registerCount;
        RegisterForm lastForm;
        final List<RegisterForm> forms = new ArrayList<>();

        @Override
        public Completable sendCode(String phone, SmsScene scene) {
            sendCount++;
            lastSendPhone = phone;
            lastScene = scene;
            return sendResult;
        }

        @Override
        public Completable verifyCode(String phone, String code) {
            verifyCount++;
            lastVerifyPhone = phone;
            lastVerifyCode = code;
            return verifyResult;
        }

        @Override
        public Single<UserEntity> register(RegisterForm form) {
            registerCount++;
            lastForm = form;
            forms.add(form);
            if (throwOnRegister) throw new UnsupportedOperationException("not implemented");
            return registerResult;
        }

        @Override
        public boolean isFakeSms() {
            return true;
        }

        @Override
        public Single<Boolean> isRegistered(String phone) {
            return Single.just(false);
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
        public Completable resetPassword(String phone, String newPassword) {
            return Completable.complete();
        }

        @Override
        public Single<String> submitAppeal(AppealEntity appeal) {
            return Single.never();
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
}
