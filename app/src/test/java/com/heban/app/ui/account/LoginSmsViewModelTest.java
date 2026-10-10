package com.heban.app.ui.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.UserRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;

public class LoginSmsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final TestScheduler clock = new TestScheduler();
    private LoginFakeUserRepository repo;
    private LoginSmsViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(s -> clock);
        repo = new LoginFakeUserRepository();
        vm = new LoginSmsViewModel(null, repo);
        vm.canSubmit.observeForever(v -> { });
        vm.canSendCode.observeForever(v -> { });
        vm.codeSentVisible.observeForever(v -> { });
        vm.showAgreementHint.observeForever(v -> { });
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    // ---- 「登录 / 注册」可点击规则 ----

    @Test
    public void canSubmit_needsElevenDigitPhoneAndSixDigitCode() {
        assertFalse(vm.canSubmit.getValue());
        vm.phone.setValue("138 0000 5678");
        vm.code.setValue("12345");
        assertFalse(vm.canSubmit.getValue());
        vm.code.setValue("123456");
        assertTrue(vm.canSubmit.getValue());
        vm.phone.setValue("138 0000 567");
        assertFalse(vm.canSubmit.getValue());
    }

    // ---- 获取验证码 ----

    @Test
    public void sendCode_invalidPhone_showsMessage() {
        vm.phone.setValue("123 4567 8901");
        vm.sendCode();
        assertEquals(Integer.valueOf(R.string.login_phone_invalid), vm.messageRes.getValue());
        assertEquals(0, repo.sendCodeCalls);
        assertTrue(vm.canSendCode.getValue());
    }

    @Test
    public void sendCode_success_startsCountdownAndShowsFakeHint() {
        vm.phone.setValue("138 0000 5678");
        vm.sendCode();
        assertEquals(1, repo.sendCodeCalls);
        assertEquals("13800005678", repo.lastPhone);
        assertEquals(SmsScene.LOGIN, repo.lastScene);
        assertEquals(Boolean.TRUE, vm.fakeSmsSent.getValue());
        assertEquals("138****5678", vm.sentTo.getValue());
        assertFalse(vm.canSendCode.getValue());

        clock.triggerActions();
        assertEquals("60s 后重发", vm.countdown.buttonText.getValue());
        assertTrue(vm.codeSentVisible.getValue());

        clock.advanceTimeBy(18, TimeUnit.SECONDS);
        assertEquals("42s 后重发", vm.countdown.buttonText.getValue());
        vm.sendCode();
        assertEquals("ignored while counting down", 1, repo.sendCodeCalls);

        clock.advanceTimeBy(42, TimeUnit.SECONDS);
        assertEquals("重新获取", vm.countdown.buttonText.getValue());
        assertTrue(vm.canSendCode.getValue());
        assertFalse(vm.codeSentVisible.getValue());
    }

    @Test
    public void sendCode_realSms_noFakeHint() {
        repo.fakeSms = false;
        vm.phone.setValue("13800005678");
        vm.sendCode();
        assertNull(vm.fakeSmsSent.getValue());
        assertFalse(vm.canSendCode.getValue());
    }

    @Test
    public void sendCode_whileRequestPending_ignoresSecondTap() {
        repo.sendCodeResult = Completable.never();
        vm.phone.setValue("13800005678");
        vm.sendCode();
        assertFalse(vm.canSendCode.getValue());
        vm.sendCode();
        assertEquals(1, repo.sendCodeCalls);
    }

    @Test
    public void sendCode_error_showsMessageAndAllowsRetry() {
        repo.sendCodeResult = Completable.error(new BusinessException("SMS_LIMIT", "发送太频繁，请稍后再试"));
        vm.phone.setValue("13800005678");
        vm.sendCode();
        assertEquals("发送太频繁，请稍后再试", vm.toast.getValue());
        assertTrue(vm.canSendCode.getValue());
        assertNull(vm.sentTo.getValue());
    }

    // ---- 登录 ----

    @Test
    public void login_withoutAgreement_showsHintAndMessage() {
        vm.phone.setValue("138 0000 5678");
        vm.code.setValue("123456");
        vm.login();
        assertTrue(vm.showAgreementHint.getValue());
        assertEquals(Integer.valueOf(R.string.agreement_required), vm.messageRes.getValue());
        assertEquals(0, repo.loginWithSmsCalls);
        vm.agreed.setValue(true);
        assertFalse(vm.showAgreementHint.getValue());
    }

    @Test
    public void login_success_goesHome() {
        vm.phone.setValue("138 0000 5678");
        vm.code.setValue("123456");
        vm.agreed.setValue(true);
        vm.login();
        assertEquals(1, repo.loginWithSmsCalls);
        assertEquals("13800005678", repo.lastPhone);
        assertEquals("123456", repo.lastCode);
        assertEquals(Boolean.TRUE, vm.goHome.getValue());
        assertFalse(vm.loading.getValue());
    }

    @Test
    public void login_codeInvalid_showsMessage() {
        repo.loginResult = Single.error(new BusinessException(UserRepository.Errors.CODE_INVALID, "验证码错误或已过期"));
        vm.phone.setValue("138 0000 5678");
        vm.code.setValue("000000");
        vm.agreed.setValue(true);
        vm.login();
        assertEquals("验证码错误或已过期", vm.toast.getValue());
        assertNull(vm.goHome.getValue());
        assertFalse(vm.loading.getValue());
    }

    @Test
    public void login_whileLoading_doesNotRequestAgain() {
        repo.loginResult = Single.never();
        vm.phone.setValue("138 0000 5678");
        vm.code.setValue("123456");
        vm.agreed.setValue(true);
        vm.login();
        vm.login();
        assertEquals(1, repo.loginWithSmsCalls);
        assertTrue(vm.loading.getValue());
    }

    @Test
    public void prefillOnce_appliesOnlyFirstTime() {
        vm.prefillOnce("13800005678", true);
        assertEquals("138 0000 5678", vm.phone.getValue());
        assertTrue(vm.agreed.getValue());
        vm.phone.setValue("");
        vm.prefillOnce("13800005678", true);
        assertEquals("", vm.phone.getValue());
    }

    @Test
    public void enterGuest_goesHome() {
        vm.enterGuest();
        assertEquals(1, repo.enterGuestCalls);
        assertEquals(Boolean.TRUE, vm.goHome.getValue());
    }
}
