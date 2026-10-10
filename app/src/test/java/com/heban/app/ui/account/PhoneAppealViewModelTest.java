package com.heban.app.ui.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.repository.UserRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.CompletableSubject;

public class PhoneAppealViewModelTest {
    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final TestScheduler time = new TestScheduler();
    private FakeUserRepository repo;
    private PhoneAppealViewModel vm;
    private final List<String> toasts = new ArrayList<>();
    private int fakeHints;

    @Before
    public void setUp() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(s -> time);
        repo = new FakeUserRepository();
        vm = new PhoneAppealViewModel(null, repo);
        observe(vm.canNext);
        observe(vm.samePhone);
        observe(vm.canSendCode);
        observe(vm.canSubmit);
        vm.toast.observeForever(toasts::add);
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

    private void toStep2() {
        vm.oldPhone.setValue("13800005678");
        vm.next();
    }

    private void fillStep2() {
        vm.newPhone.setValue("13900002468");
        vm.code.setValue("123456");
    }

    @Test
    public void defaults() {
        assertEquals(PhoneAppealViewModel.STEP_ACCOUNT, (int) vm.step.getValue());
        assertEquals("一个月内", vm.lastLogin.getValue());
        assertEquals("江苏 南京", vm.region.getValue());
        assertFalse(vm.canNext.getValue());
        assertFalse(vm.back());
    }

    @Test
    public void step1RequiresValidOldPhone() {
        vm.oldPhone.setValue("1380000567");
        assertFalse(vm.canNext.getValue());
        vm.next();
        assertEquals(PhoneAppealViewModel.STEP_ACCOUNT, (int) vm.step.getValue());

        vm.oldPhone.setValue("13800005678");
        assertTrue(vm.canNext.getValue());
        vm.next();
        assertEquals(PhoneAppealViewModel.STEP_NEW_PHONE, (int) vm.step.getValue());
    }

    @Test
    public void backFromStep2ReturnsToStep1KeepingInput() {
        toStep2();
        vm.nickname.setValue("老李种菜");
        assertTrue(vm.back());
        assertEquals(PhoneAppealViewModel.STEP_ACCOUNT, (int) vm.step.getValue());
        assertEquals("13800005678", vm.oldPhone.getValue());
        assertEquals("老李种菜", vm.nickname.getValue());
        assertFalse(vm.back());
    }

    @Test
    public void newPhoneMustDifferFromOldPhone() {
        toStep2();
        vm.newPhone.setValue("13800005678");
        vm.code.setValue("123456");
        assertTrue(vm.samePhone.getValue());
        assertFalse(vm.canSendCode.getValue());
        assertFalse(vm.canSubmit.getValue());
        vm.submit();
        assertEquals(0, repo.count("verifyCode"));

        vm.newPhone.setValue("13900002468");
        assertFalse(vm.samePhone.getValue());
        assertTrue(vm.canSendCode.getValue());
        assertTrue(vm.canSubmit.getValue());

        // 回到第 1 步把原手机号改成与新手机号相同，同样不能提交
        vm.back();
        vm.oldPhone.setValue("13900002468");
        vm.next();
        assertTrue(vm.samePhone.getValue());
        assertFalse(vm.canSubmit.getValue());
    }

    @Test
    public void sendCodeUsesChangePhoneSceneAndMasksNumber() {
        toStep2();
        vm.newPhone.setValue("13900002468");
        vm.sendCode();

        assertEquals("13900002468", repo.lastPhone);
        assertEquals(SmsScene.CHANGE_PHONE, repo.lastScene);
        assertEquals("139****2468", vm.sentMasked.getValue());
        assertFalse(vm.canSendCode.getValue());
        time.advanceTimeBy(18, TimeUnit.SECONDS);
        assertEquals("42s 后重发", vm.countdown.buttonText.getValue());
        vm.sendCode();
        assertEquals(1, repo.count("sendCode"));
        assertEquals(0, fakeHints);
    }

    @Test
    public void sendCodeFakeAndError() {
        toStep2();
        vm.newPhone.setValue("13900002468");
        repo.fakeSms = true;
        repo.sendResult = Completable.error(new BusinessException("isv.BUSINESS_LIMIT_CONTROL", "发送太频繁，请稍后再试"));
        vm.sendCode();
        assertEquals("发送太频繁，请稍后再试", toasts.get(0));
        assertNull(vm.sentMasked.getValue());
        assertEquals(0, fakeHints);

        repo.sendResult = Completable.complete();
        vm.sendCode();
        assertEquals(1, fakeHints);
    }

    @Test
    public void submitVerifiesCodeThenSavesAppeal() {
        vm.oldPhone.setValue("13800005678");
        vm.nickname.setValue("  老李种菜 ");
        vm.lastLogin.setValue("一周内");
        vm.region.setValue("山东 寿光");
        vm.next();
        fillStep2();
        vm.submit();

        assertEquals(List.of("verifyCode", "submitAppeal"), repo.calls);
        assertEquals("123456", repo.lastCode);
        AppealEntity a = repo.lastAppeal;
        assertEquals("13800005678", a.oldPhone);
        assertEquals("13900002468", a.newPhone);
        assertEquals("老李种菜", a.nickname);
        assertEquals("一周内", a.lastLogin);
        assertEquals("山东 寿光", a.region);
        assertEquals(AppealEntity.STATUS_PENDING, a.status);
        assertTrue(a.createdAt > 0);

        assertEquals(PhoneAppealViewModel.STEP_SUBMITTED, (int) vm.step.getValue());
        assertEquals("HB20261010-0832", vm.appealNo.getValue());
        assertEquals("138****5678", vm.submittedOldMasked.getValue());
        assertEquals("139****2468", vm.submittedNewMasked.getValue());
        assertFalse(vm.back());
    }

    @Test
    public void blankNicknameIsSavedAsNull() {
        toStep2();
        vm.nickname.setValue("   ");
        fillStep2();
        vm.submit();
        assertNull(repo.lastAppeal.nickname);
        assertEquals("一个月内", repo.lastAppeal.lastLogin);
        assertEquals("江苏 南京", repo.lastAppeal.region);
    }

    @Test
    public void wrongCodeDoesNotSubmitAppeal() {
        repo.verifyResult = Completable.error(new BusinessException(UserRepository.Errors.CODE_INVALID, "验证码错误或已过期"));
        toStep2();
        fillStep2();
        vm.submit();

        assertEquals(0, repo.count("submitAppeal"));
        assertEquals(PhoneAppealViewModel.STEP_NEW_PHONE, (int) vm.step.getValue());
        assertEquals("验证码错误或已过期", toasts.get(0));
        assertFalse(vm.submitting.getValue());
    }

    @Test
    public void submitErrorStaysOnStep2() {
        repo.appealResult = Single.error(new BusinessException(UserRepository.Errors.SAME_PHONE, "新手机号不能与原手机号相同"));
        toStep2();
        fillStep2();
        vm.submit();
        assertEquals(PhoneAppealViewModel.STEP_NEW_PHONE, (int) vm.step.getValue());
        assertEquals("新手机号不能与原手机号相同", toasts.get(0));
    }

    @Test
    public void submitIgnoresDoubleTapAndBackWhileSubmitting() {
        CompletableSubject pending = CompletableSubject.create();
        repo.verifyResult = pending;
        toStep2();
        fillStep2();
        vm.submit();
        vm.submit();
        assertEquals(1, repo.count("verifyCode"));
        assertTrue(vm.submitting.getValue());
        assertFalse(vm.back());

        pending.onComplete();
        assertEquals(1, repo.count("submitAppeal"));
        assertEquals(PhoneAppealViewModel.STEP_SUBMITTED, (int) vm.step.getValue());
        assertFalse(vm.submitting.getValue());
    }
}
