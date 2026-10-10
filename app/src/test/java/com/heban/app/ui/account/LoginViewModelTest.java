package com.heban.app.ui.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.repository.UserRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private LoginFakeUserRepository repo;
    private LoginViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new LoginFakeUserRepository();
        vm = new LoginViewModel(null, repo);
        vm.canSubmit.observeForever(v -> { });
        vm.showAgreementHint.observeForever(v -> { });
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private void fill(String phone, String password, boolean agreed) {
        vm.phone.setValue(phone);
        vm.password.setValue(password);
        vm.agreed.setValue(agreed);
    }

    // ---- 「登录」可点击规则 ----

    @Test
    public void canSubmit_falseInitially() {
        assertFalse(vm.canSubmit.getValue());
    }

    @Test
    public void canSubmit_needsElevenDigitsAndPassword() {
        fill("138 0000 567", "heban2026", false);
        assertFalse(vm.canSubmit.getValue());

        fill("138 0000 5678", "", false);
        assertFalse(vm.canSubmit.getValue());

        fill("138 0000 5678", "heban2026", false);
        assertTrue(vm.canSubmit.getValue());

        fill("13800005678", "x", false);
        assertTrue(vm.canSubmit.getValue());
    }

    @Test
    public void login_ignoredWhenFormIncomplete() {
        fill("1380000", "heban2026", true);
        vm.login();
        assertEquals(0, repo.loginWithPasswordCalls);
        assertNull(vm.messageRes.getValue());
    }

    // ---- 协议勾选 ----

    @Test
    public void login_withoutAgreement_showsHintAndMessage() {
        fill("138 0000 5678", "heban2026", false);
        vm.login();
        assertTrue(vm.showAgreementHint.getValue());
        assertEquals(Integer.valueOf(R.string.agreement_required), vm.messageRes.getValue());
        assertEquals(0, repo.loginWithPasswordCalls);
    }

    @Test
    public void checkingAgreement_hidesHint() {
        fill("138 0000 5678", "heban2026", false);
        vm.login();
        vm.agreed.setValue(true);
        assertFalse(vm.showAgreementHint.getValue());
    }

    // ---- 登录 ----

    @Test
    public void login_invalidPhone_showsMessage() {
        fill("123 4567 8901", "heban2026", true);
        vm.login();
        assertEquals(Integer.valueOf(R.string.login_phone_invalid), vm.messageRes.getValue());
        assertEquals(0, repo.loginWithPasswordCalls);
    }

    @Test
    public void login_success_passesDigitsAndGoesHome() {
        fill("138 0000 5678", "heban2026", true);
        vm.login();
        assertEquals(1, repo.loginWithPasswordCalls);
        assertEquals("13800005678", repo.lastPhone);
        assertEquals("heban2026", repo.lastPassword);
        assertEquals(Boolean.TRUE, vm.goHome.getValue());
        assertFalse(vm.loading.getValue());
    }

    @Test
    public void login_whileLoading_doesNotRequestAgain() {
        repo.loginResult = Single.never();
        fill("138 0000 5678", "heban2026", true);
        vm.login();
        assertTrue(vm.loading.getValue());
        vm.login();
        vm.login();
        assertEquals(1, repo.loginWithPasswordCalls);
    }

    @Test
    public void login_businessErrors_showRepositoryMessage() {
        String[][] cases = {
                {UserRepository.Errors.WRONG_PASSWORD, "账号或密码错误，还可以尝试 3 次"},
                {UserRepository.Errors.LOCKED, "密码连续输错 5 次，请 10 分钟后再试"},
                {UserRepository.Errors.NOT_REGISTERED, "该手机号未注册"},
                {UserRepository.Errors.NO_PASSWORD, "账号未设置密码，请用验证码登录"},
        };
        fill("138 0000 5678", "heban2026", true);
        for (String[] c : cases) {
            repo.loginResult = Single.error(new BusinessException(c[0], c[1]));
            vm.login();
            assertEquals(c[1], vm.toast.getValue());
            assertFalse(vm.loading.getValue());
            assertNull(vm.goHome.getValue());
        }
        assertEquals(cases.length, repo.loginWithPasswordCalls);
    }

    @Test
    public void login_networkError_showsAppErrorMessage() {
        repo.loginResult = Single.error(new IOException("offline"));
        fill("138 0000 5678", "heban2026", true);
        vm.login();
        assertEquals("网络不太好，检查后再试一次", vm.toast.getValue());
        assertFalse(vm.loading.getValue());
    }

    // ---- 游客浏览 ----

    @Test
    public void enterGuest_goesHomeOnce() {
        vm.enterGuest();
        vm.enterGuest();
        assertEquals(1, repo.enterGuestCalls);
        assertEquals(Boolean.TRUE, vm.goHome.getValue());
    }

    // ---- 带回的手机号 / 输入格式 ----

    @Test
    public void applyFromOtherTab_formatsPhone() {
        vm.applyFromOtherTab("13800005678", true);
        assertEquals("138 0000 5678", vm.phone.getValue());
        assertTrue(vm.agreed.getValue());
    }

    @Test
    public void phoneFormat_groupsThreeFourFour() {
        assertEquals("", LoginPhoneInput.format(""));
        assertEquals("138", LoginPhoneInput.format("138"));
        assertEquals("138 0", LoginPhoneInput.format("1380"));
        assertEquals("138 0000", LoginPhoneInput.format("1380000"));
        assertEquals("138 0000 5", LoginPhoneInput.format("13800005"));
        assertEquals("138 0000 5678", LoginPhoneInput.format("138000056789"));
        assertEquals("13800005678", LoginPhoneInput.digits("138 0000 5678"));
    }
}
