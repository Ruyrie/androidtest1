package com.heban.app.ui.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.util.PasswordStrength;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.CompletableSubject;

public class ResetPwdViewModelTest {
    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final TestScheduler time = new TestScheduler();
    private FakeUserRepository repo;
    private ResetPwdViewModel vm;
    private final List<String> toasts = new ArrayList<>();
    private int navigations;
    private int fakeHints;

    @Before
    public void setUp() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(s -> time);
        repo = new FakeUserRepository();
        vm = new ResetPwdViewModel(null, repo);
        // MediatorLiveData 只有在被观察时才会更新
        observe(vm.canSendCode);
        observe(vm.canNext);
        observe(vm.strength);
        observe(vm.lengthRule);
        observe(vm.mixRule);
        observe(vm.recentRule);
        observe(vm.confirm);
        observe(vm.canFinish);
        vm.toast.observeForever(toasts::add);
        vm.toResetPwd.observeForever(b -> navigations++);
        vm.fakeSmsSent.observeForever(b -> fakeHints++);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private static <T> void observe(LiveData<T> live) {
        live.observeForever(v -> { });
    }

    private void verifyStep1() {
        vm.phone.setValue("138 0000 5678");
        vm.code.setValue("123456");
        vm.next();
    }

    // ---------- A07 验证手机 ----------

    @Test
    public void sendCodeEnabledOnlyForValidPhone() {
        assertFalse(vm.canSendCode.getValue());
        vm.phone.setValue("138 0000 567");
        assertFalse(vm.canSendCode.getValue());
        vm.phone.setValue("128 0000 5678");
        assertFalse(vm.canSendCode.getValue());
        vm.phone.setValue("138 0000 5678");
        assertTrue(vm.canSendCode.getValue());
    }

    @Test
    public void sendCodeUsesDigitsAndResetSceneThenMasksAndCountsDown() {
        vm.phone.setValue("138 0000 5678");
        vm.sendCode();

        assertEquals("13800005678", repo.lastPhone);
        assertEquals(SmsScene.RESET_PASSWORD, repo.lastScene);
        assertEquals("138****5678", vm.sentMasked.getValue());
        assertFalse(vm.sending.getValue());
        assertFalse(vm.canSendCode.getValue());

        time.advanceTimeBy(4, TimeUnit.SECONDS);
        assertEquals("56s 后重发", vm.countdown.buttonText.getValue());

        vm.sendCode();   // 倒计时中不会重复发送
        assertEquals(1, repo.count("sendCode"));

        time.advanceTimeBy(56, TimeUnit.SECONDS);
        assertEquals("重新获取", vm.countdown.buttonText.getValue());
        assertTrue(vm.canSendCode.getValue());
        assertEquals(0, fakeHints);
    }

    @Test
    public void fakeSmsShowsHint() {
        repo.fakeSms = true;
        vm.phone.setValue("13800005678");
        vm.sendCode();
        assertEquals(1, fakeHints);
    }

    @Test
    public void sendCodeErrorShowsMessage() {
        repo.sendResult = Completable.error(new BusinessException(UserRepository.Errors.NOT_REGISTERED, "该手机号未注册"));
        vm.phone.setValue("13800005678");
        vm.sendCode();

        assertEquals(1, toasts.size());
        assertEquals("该手机号未注册", toasts.get(0));
        assertNull(vm.sentMasked.getValue());
        assertFalse(vm.sending.getValue());
        assertTrue(vm.countdown.canSend.getValue());
        assertTrue(vm.canSendCode.getValue());
    }

    @Test
    public void nextRequiresValidPhoneAndSixDigitCode() {
        vm.phone.setValue("138 0000 5678");
        vm.code.setValue("4829");
        assertFalse(vm.canNext.getValue());
        vm.next();
        assertEquals(0, repo.count("verifyCode"));

        vm.code.setValue("123456");
        assertTrue(vm.canNext.getValue());
        vm.next();
        assertEquals("13800005678", repo.lastPhone);
        assertEquals("123456", repo.lastCode);
        assertEquals(1, navigations);
        assertEquals("13800005678", vm.verifiedPhone());
    }

    @Test
    public void wrongCodeStaysOnStep1() {
        repo.verifyResult = Completable.error(new BusinessException(UserRepository.Errors.CODE_INVALID, "验证码错误或已过期"));
        verifyStep1();
        assertEquals(0, navigations);
        assertEquals("验证码错误或已过期", toasts.get(0));
        assertFalse(vm.verifying.getValue());
    }

    @Test
    public void nextIgnoresDoubleTapWhileVerifying() {
        CompletableSubject pending = CompletableSubject.create();
        repo.verifyResult = pending;
        verifyStep1();
        vm.next();
        assertEquals(1, repo.count("verifyCode"));
        assertTrue(vm.verifying.getValue());

        pending.onComplete();
        assertFalse(vm.verifying.getValue());
        assertEquals(1, navigations);
    }

    // ---------- A08 设置新密码 ----------

    @Test
    public void passwordRulesFollowInput() {
        assertEquals(ResetPwdViewModel.Rule.UNMET, vm.lengthRule.getValue());
        assertEquals(ResetPwdViewModel.Rule.UNMET, vm.mixRule.getValue());
        assertEquals(ResetPwdViewModel.Rule.PENDING, vm.recentRule.getValue());
        assertEquals(PasswordStrength.Level.NONE, vm.strength.getValue().level);

        vm.newPwd.setValue("abcdefgh");
        assertEquals(ResetPwdViewModel.Rule.MET, vm.lengthRule.getValue());
        assertEquals(ResetPwdViewModel.Rule.UNMET, vm.mixRule.getValue());

        vm.newPwd.setValue("abc123");
        assertEquals(ResetPwdViewModel.Rule.UNMET, vm.lengthRule.getValue());
        assertEquals(ResetPwdViewModel.Rule.MET, vm.mixRule.getValue());

        vm.newPwd.setValue("abc123456789012345678");   // 21 位
        assertEquals(ResetPwdViewModel.Rule.UNMET, vm.lengthRule.getValue());

        vm.newPwd.setValue("Heban@2026");
        assertEquals(ResetPwdViewModel.Rule.MET, vm.lengthRule.getValue());
        assertEquals(ResetPwdViewModel.Rule.MET, vm.mixRule.getValue());
        assertEquals("强", vm.strength.getValue().label());
        assertEquals(ResetPwdViewModel.Rule.PENDING, vm.recentRule.getValue());
    }

    @Test
    public void confirmStateAndFinishEnabled() {
        vm.newPwd.setValue("heban2026");
        assertEquals(ResetPwdViewModel.Confirm.EMPTY, vm.confirm.getValue());
        assertFalse(vm.canFinish.getValue());

        vm.confirmPwd.setValue("heban202");
        assertEquals(ResetPwdViewModel.Confirm.MISMATCH, vm.confirm.getValue());
        assertFalse(vm.canFinish.getValue());

        vm.confirmPwd.setValue("heban2026");
        assertEquals(ResetPwdViewModel.Confirm.MATCH, vm.confirm.getValue());
        assertTrue(vm.canFinish.getValue());

        // 两次一致但不满足规则
        vm.newPwd.setValue("abcdefgh");
        vm.confirmPwd.setValue("abcdefgh");
        assertEquals(ResetPwdViewModel.Confirm.MATCH, vm.confirm.getValue());
        assertFalse(vm.canFinish.getValue());
    }

    @Test
    public void finishResetsPasswordForVerifiedPhone() {
        verifyStep1();
        vm.phone.setValue("139 0000 2468");   // 返回 A07 改了号码也以验证通过的号码为准
        vm.newPwd.setValue("Heban@2026");
        vm.confirmPwd.setValue("Heban@2026");
        vm.finish();

        assertEquals(1, repo.count("resetPassword"));
        assertEquals("13800005678", repo.lastPhone);
        assertEquals("Heban@2026", repo.lastPassword);
        assertTrue(vm.done.getValue());
        assertFalse(vm.resetting.getValue());
    }

    @Test
    public void sameAsRecentMarksRuleFailedUntilPasswordChanges() {
        repo.resetResult = Completable.error(new BusinessException(UserRepository.Errors.SAME_AS_RECENT, "新密码不能与最近使用过的密码相同"));
        verifyStep1();
        vm.newPwd.setValue("Heban@2026");
        vm.confirmPwd.setValue("Heban@2026");
        vm.finish();

        assertEquals(ResetPwdViewModel.Rule.FAILED, vm.recentRule.getValue());
        assertFalse(vm.canFinish.getValue());
        assertFalse(vm.done.getValue());
        assertEquals("新密码不能与最近使用过的密码相同", toasts.get(0));

        vm.finish();   // 规则未通过时不会再次提交
        assertEquals(1, repo.count("resetPassword"));

        vm.newPwd.setValue("Heban@2027");
        assertEquals(ResetPwdViewModel.Rule.PENDING, vm.recentRule.getValue());
        vm.confirmPwd.setValue("Heban@2027");
        assertTrue(vm.canFinish.getValue());

        vm.newPwd.setValue("Heban@2026");
        assertEquals(ResetPwdViewModel.Rule.FAILED, vm.recentRule.getValue());

        repo.resetResult = Completable.complete();
        vm.newPwd.setValue("Heban@2027");
        vm.finish();
        assertTrue(vm.done.getValue());
    }

    @Test
    public void otherErrorsKeepRecentRulePending() {
        repo.resetResult = Completable.error(new IOException("offline"));
        verifyStep1();
        vm.newPwd.setValue("Heban@2026");
        vm.confirmPwd.setValue("Heban@2026");
        vm.finish();

        assertEquals(ResetPwdViewModel.Rule.PENDING, vm.recentRule.getValue());
        assertTrue(vm.canFinish.getValue());
        assertEquals("网络不太好，检查后再试一次", toasts.get(0));
    }

    @Test
    public void finishIgnoresDoubleTap() {
        CompletableSubject pending = CompletableSubject.create();
        repo.resetResult = pending;
        verifyStep1();
        vm.newPwd.setValue("Heban@2026");
        vm.confirmPwd.setValue("Heban@2026");
        vm.finish();
        vm.finish();
        assertEquals(1, repo.count("resetPassword"));
        assertTrue(vm.resetting.getValue());
        pending.onComplete();
        assertTrue(vm.done.getValue());
    }

    @Test
    public void strengthSegments() {
        assertEquals(0, ResetPwdViewModel.strengthSegments(PasswordStrength.evaluate("")));
        assertEquals(1, ResetPwdViewModel.strengthSegments(PasswordStrength.evaluate("abc")));
        assertEquals(2, ResetPwdViewModel.strengthSegments(PasswordStrength.evaluate("heban2026")));
        assertEquals(3, ResetPwdViewModel.strengthSegments(PasswordStrength.evaluate("Heban@2026")));
        assertEquals(0, ResetPwdViewModel.strengthSegments(null));
    }

    @Test
    public void phoneSpacingFormat() {
        assertEquals("138", ResetUi.PhoneSpacingWatcher.format("138"));
        assertEquals("138 0", ResetUi.PhoneSpacingWatcher.format("1380"));
        assertEquals("138 0000", ResetUi.PhoneSpacingWatcher.format("1380000"));
        assertEquals("138 0000 5", ResetUi.PhoneSpacingWatcher.format("13800005"));
        assertEquals("138 0000 5678", ResetUi.PhoneSpacingWatcher.format("13800005678"));
        assertEquals("13800005678", ResetPwdViewModel.digits("138 0000 5678"));
    }
}
