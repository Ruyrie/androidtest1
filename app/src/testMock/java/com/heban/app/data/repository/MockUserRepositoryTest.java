package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.local.prefs.TokenCipher;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.testutil.FakeAppealDao;
import com.heban.app.testutil.FakeSessionStore;
import com.heban.app.testutil.FakeUserDao;
import com.heban.app.testutil.RecordingSmsService;
import com.heban.app.util.PasswordHasher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class MockUserRepositoryTest {
    private static final String PHONE = "13800005678";
    private static final String PASSWORD = "abc12345";
    private static final String CODE = "123456";
    private static final long MINUTE = 60_000L;
    private static final long DAY = 24 * 60 * MINUTE;

    private FakeUserDao userDao;
    private FakeAppealDao appealDao;
    private FakeSessionStore session;
    private RecordingSmsService sms;
    private long now = 1_791_621_000_000L;   // 2026-10-10T08:30:00Z
    private boolean cipherFails;
    private MockUserRepository repo;

    @Before
    public void setUp() {
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(s -> Schedulers.trampoline());
        userDao = new FakeUserDao();
        appealDao = new FakeAppealDao();
        session = new FakeSessionStore();
        sms = new RecordingSmsService();
        TokenCipher cipher = new TokenCipher() {
            @Override
            public String encrypt(String plain) throws Exception {
                if (cipherFails) throw new java.security.KeyStoreException("AndroidKeyStore not found");
                return "enc:" + plain;
            }

            @Override
            public String decrypt(String encoded) {
                return encoded.substring(4);
            }
        };
        repo = new MockUserRepository(userDao, appealDao, session, cipher, sms, () -> now);
    }

    @After
    public void tearDown() {
        RxJavaPlugins.reset();
    }

    // —— 测试数据 ——

    private UserEntity seedUser(String phone, String password) {
        UserEntity u = new UserEntity();
        u.phone = phone;
        u.nickname = "老张";
        if (password != null) {
            u.salt = PasswordHasher.newSalt();
            u.passwordHash = PasswordHasher.hash(password, u.salt);
        }
        u.createdAt = now - 30 * DAY;
        return userDao.seed(u);
    }

    private static void assertBusiness(TestObserver<?> t, String code, String message) {
        t.assertNotComplete();
        t.assertError(e -> {
            assertTrue("应为 BusinessException：" + e, e instanceof BusinessException);
            assertEquals(code, ((BusinessException) e).code);
            if (message != null) assertEquals(message, e.getMessage());
            return true;
        });
    }

    private static <T> TestObserver<T> run(Single<T> s) {
        return s.test();
    }

    private static TestObserver<Void> run(Completable c) {
        return c.test();
    }

    // —— 发送 / 校验验证码 ——

    @Test
    public void sendCode_invalidPhone() {
        assertBusiness(run(repo.sendCode("1380000", SmsScene.LOGIN)), "INVALID_PHONE", "请输入正确的手机号");
        assertTrue(sms.sent.isEmpty());
    }

    @Test
    public void sendCode_registerWithExistingPhone_phoneExists() {
        seedUser(PHONE, PASSWORD);
        assertBusiness(run(repo.sendCode(PHONE, SmsScene.REGISTER)),
                UserRepository.Errors.PHONE_EXISTS, "该手机号已注册，请直接登录");
        assertTrue(sms.sent.isEmpty());
    }

    @Test
    public void sendCode_resetWithUnknownPhone_notRegistered() {
        assertBusiness(run(repo.sendCode(PHONE, SmsScene.RESET_PASSWORD)),
                UserRepository.Errors.NOT_REGISTERED, "该手机号未注册");
        assertTrue(sms.sent.isEmpty());
    }

    @Test
    public void sendCode_validCases_send() {
        run(repo.sendCode(PHONE, SmsScene.LOGIN)).assertComplete();
        run(repo.sendCode(PHONE, SmsScene.REGISTER)).assertComplete();
        seedUser(PHONE, PASSWORD);
        run(repo.sendCode(PHONE, SmsScene.RESET_PASSWORD)).assertComplete();
        assertEquals(Arrays.asList(SmsScene.LOGIN, SmsScene.REGISTER, SmsScene.RESET_PASSWORD), sms.scenes);
    }

    @Test
    public void verifyCode() {
        run(repo.verifyCode(PHONE, CODE)).assertComplete();
        assertBusiness(run(repo.verifyCode(PHONE, "000000")),
                UserRepository.Errors.CODE_INVALID, "验证码错误或已过期");
        assertBusiness(run(repo.verifyCode(PHONE, "")), UserRepository.Errors.CODE_INVALID, null);
    }

    @Test
    public void isRegistered() {
        run(repo.isRegistered(PHONE)).assertValue(false);
        seedUser(PHONE, PASSWORD);
        run(repo.isRegistered(PHONE)).assertValue(true);
    }

    // —— 密码登录 ——

    @Test
    public void loginWithPassword_notRegistered() {
        assertBusiness(run(repo.loginWithPassword(PHONE, PASSWORD)),
                UserRepository.Errors.NOT_REGISTERED, "该手机号未注册");
        assertEquals(0, session.userId());
    }

    @Test
    public void loginWithPassword_noPassword() {
        seedUser(PHONE, null);
        assertBusiness(run(repo.loginWithPassword(PHONE, PASSWORD)),
                UserRepository.Errors.NO_PASSWORD, "该账号未设置密码，请使用验证码登录");
    }

    @Test
    public void loginWithPassword_success_writesSession() {
        UserEntity seeded = seedUser(PHONE, PASSWORD);
        TestObserver<UserEntity> t = run(repo.loginWithPassword(PHONE, PASSWORD));
        t.assertComplete();
        assertEquals(seeded.id, t.values().get(0).id);
        assertEquals(seeded.id, session.userId());
        assertNotNull(session.token);
        assertTrue(session.token.startsWith("enc:"));
        assertFalse(session.guest);
        assertTrue(session.onboardingDone);
    }

    @Test
    public void loginWithPassword_wrongPasswordCountsDownThenLocks() {
        UserEntity seeded = seedUser(PHONE, PASSWORD);
        for (int left = 4; left >= 1; left--) {
            assertBusiness(run(repo.loginWithPassword(PHONE, "wrong1234")),
                    UserRepository.Errors.WRONG_PASSWORD, "账号或密码错误，还可尝试 " + left + " 次");
        }
        assertEquals(4, userDao.rows.get(seeded.id).failedAttempts);

        // 第 5 次：锁定 10 分钟
        assertBusiness(run(repo.loginWithPassword(PHONE, "wrong1234")),
                UserRepository.Errors.LOCKED, "密码错误次数过多，请 10 分钟后再试");
        assertEquals(now + 10 * MINUTE, userDao.rows.get(seeded.id).lockedUntil);

        // 锁定期间正确密码也不能登录，提示剩余分钟数（向上取整）
        now += 3 * MINUTE + 30_000;
        assertBusiness(run(repo.loginWithPassword(PHONE, PASSWORD)),
                UserRepository.Errors.LOCKED, "密码错误次数过多，请 7 分钟后再试");
        assertEquals(0, session.userId());

        // 10 分钟后解锁，登录成功并清零计数
        now += 7 * MINUTE;
        run(repo.loginWithPassword(PHONE, PASSWORD)).assertComplete();
        UserEntity row = userDao.rows.get(seeded.id);
        assertEquals(0, row.failedAttempts);
        assertEquals(0, row.lockedUntil);
        assertEquals(seeded.id, session.userId());
    }

    @Test
    public void loginWithPassword_successResetsFailedAttempts() {
        UserEntity seeded = seedUser(PHONE, PASSWORD);
        run(repo.loginWithPassword(PHONE, "wrong1234"));
        run(repo.loginWithPassword(PHONE, "wrong1234"));
        run(repo.loginWithPassword(PHONE, PASSWORD)).assertComplete();
        assertEquals(0, userDao.rows.get(seeded.id).failedAttempts);
        assertBusiness(run(repo.loginWithPassword(PHONE, "wrong1234")),
                UserRepository.Errors.WRONG_PASSWORD, "账号或密码错误，还可尝试 4 次");
    }

    @Test
    public void loginWithPassword_softDeletedWithin7Days_restores() {
        UserEntity u = seedUser(PHONE, PASSWORD);
        u.deletedAt = now - 6 * DAY;
        userDao.seed(u);
        run(repo.loginWithPassword(PHONE, PASSWORD)).assertComplete();
        assertEquals(0, userDao.rows.get(u.id).deletedAt);
        assertEquals(u.id, session.userId());
    }

    @Test
    public void loginWithPassword_deletedMoreThan7DaysAgo_notRegistered() {
        UserEntity u = seedUser(PHONE, PASSWORD);
        u.deletedAt = now - 8 * DAY;
        userDao.seed(u);
        assertBusiness(run(repo.loginWithPassword(PHONE, PASSWORD)),
                UserRepository.Errors.NOT_REGISTERED, "该手机号未注册");
        run(repo.isRegistered(PHONE)).assertValue(false);
    }

    @Test
    public void login_tokenEncryptionFails_stillLogsIn() {
        UserEntity seeded = seedUser(PHONE, PASSWORD);
        cipherFails = true;
        run(repo.loginWithPassword(PHONE, PASSWORD)).assertComplete();
        assertEquals(seeded.id, session.userId());
        assertNull(session.token);
    }

    // —— 验证码登录 ——

    @Test
    public void loginWithSms_unknownPhone_autoRegisters() {
        TestObserver<UserEntity> t = run(repo.loginWithSms(PHONE, CODE));
        t.assertComplete();
        UserEntity u = t.values().get(0);
        assertTrue(u.id > 0);
        assertEquals("农友5678", u.nickname);
        assertNull(u.passwordHash);
        assertEquals(now, u.createdAt);
        assertEquals(1, userDao.rows.size());
        assertEquals(u.id, session.userId());
        assertTrue(session.onboardingDone);

        // 再次验证码登录不会重复创建
        repo.logout().test().assertComplete();
        TestObserver<UserEntity> again = run(repo.loginWithSms(PHONE, CODE));
        assertEquals(u.id, again.values().get(0).id);
        assertEquals(1, userDao.rows.size());

        // 自动创建的账号没有密码
        assertBusiness(run(repo.loginWithPassword(PHONE, PASSWORD)), UserRepository.Errors.NO_PASSWORD, null);
    }

    @Test
    public void loginWithSms_existingUser_logsIntoThatAccount() {
        UserEntity seeded = seedUser(PHONE, PASSWORD);
        TestObserver<UserEntity> t = run(repo.loginWithSms(PHONE, CODE));
        assertEquals(seeded.id, t.values().get(0).id);
        assertEquals("老张", t.values().get(0).nickname);
    }

    @Test
    public void loginWithSms_wrongCode_noAccountCreated() {
        assertBusiness(run(repo.loginWithSms(PHONE, "000000")), UserRepository.Errors.CODE_INVALID, null);
        assertTrue(userDao.rows.isEmpty());
        assertEquals(0, session.userId());
    }

    @Test
    public void loginWithSms_softDeletedWithin7Days_restores() {
        UserEntity u = seedUser(PHONE, PASSWORD);
        u.deletedAt = now - DAY;
        userDao.seed(u);
        run(repo.loginWithSms(PHONE, CODE)).assertComplete();
        assertEquals(0, userDao.rows.get(u.id).deletedAt);
        assertEquals("老张", userDao.rows.get(u.id).nickname);
    }

    @Test
    public void loginWithSms_deletedMoreThan7DaysAgo_createsFreshAccount() {
        UserEntity u = seedUser(PHONE, PASSWORD);
        u.deletedAt = now - 10 * DAY;
        userDao.seed(u);
        TestObserver<UserEntity> t = run(repo.loginWithSms(PHONE, CODE));
        t.assertComplete();
        UserEntity row = userDao.byPhone(PHONE);
        assertEquals("农友5678", row.nickname);
        assertNull(row.passwordHash);
        assertEquals(0, row.deletedAt);
        assertEquals(1, userDao.rows.size());
    }

    // —— 注册 ——

    private static RegisterForm form(String nickname) {
        RegisterForm f = new RegisterForm();
        f.phone = PHONE;
        f.password = PASSWORD;
        f.nickname = nickname;
        f.region = "江苏省 南京市 江宁区";
        f.identity = "种植户";
        f.crops.addAll(Arrays.asList("番茄", " 黄瓜 ", "", "小麦"));
        return f;
    }

    @Test
    public void register_success() {
        TestObserver<UserEntity> t = run(repo.register(form("  种菜的老王 ")));
        t.assertComplete();
        UserEntity row = userDao.byPhone(PHONE);
        assertEquals("种菜的老王", row.nickname);
        assertEquals("江苏省 南京市 江宁区", row.region);
        assertEquals("种植户", row.identity);
        assertEquals("番茄,黄瓜,小麦", row.crops);
        assertEquals(now, row.createdAt);
        assertTrue(PasswordHasher.verify(PASSWORD, row.salt, row.passwordHash));
        assertFalse(PASSWORD.equals(row.passwordHash));
        assertEquals(row.id, session.userId());
        assertTrue(session.onboardingDone);
        assertNotNull(session.token);

        // 注册后可以用密码登录
        run(repo.logout()).assertComplete();
        run(repo.loginWithPassword(PHONE, PASSWORD)).assertComplete();
    }

    @Test
    public void register_skippedProfile_defaultNickname() {
        RegisterForm f = form(null);
        f.region = null;
        f.identity = null;
        f.crops.clear();
        run(repo.register(f)).assertComplete();
        UserEntity row = userDao.byPhone(PHONE);
        assertEquals("农友5678", row.nickname);
        assertNull(row.crops);
        assertNull(row.region);

        RegisterForm blank = form("   ");
        blank.phone = "13900001234";
        run(repo.register(blank)).assertComplete();
        assertEquals("农友1234", userDao.byPhone("13900001234").nickname);
    }

    @Test
    public void register_phoneExists() {
        seedUser(PHONE, "other1234");
        assertBusiness(run(repo.register(form("x"))),
                UserRepository.Errors.PHONE_EXISTS, "该手机号已注册，请直接登录");
        assertEquals(0, session.userId());
    }

    @Test
    public void register_weakPassword() {
        RegisterForm f = form(null);
        f.password = "12345678";
        assertBusiness(run(repo.register(f)), "WEAK_PASSWORD", "密码需为 8–20 位，并同时包含字母和数字");
        assertTrue(userDao.rows.isEmpty());
    }

    @Test
    public void register_overAccountDeletedMoreThan7DaysAgo() {
        UserEntity old = seedUser(PHONE, "old12345");
        old.deletedAt = now - 30 * DAY;
        userDao.seed(old);
        run(repo.register(form("新账号"))).assertComplete();
        assertEquals(1, userDao.rows.size());
        UserEntity row = userDao.byPhone(PHONE);
        assertEquals("新账号", row.nickname);
        assertEquals(0, row.deletedAt);
        assertTrue(PasswordHasher.verify(PASSWORD, row.salt, row.passwordHash));
        assertNull(row.prevPasswordHash);
    }

    // —— 重置密码 ——

    @Test
    public void resetPassword_sameAsCurrent() {
        seedUser(PHONE, PASSWORD);
        assertBusiness(run(repo.resetPassword(PHONE, PASSWORD)),
                UserRepository.Errors.SAME_AS_RECENT, "新密码不能与最近使用过的密码相同");
    }

    @Test
    public void resetPassword_sameAsPrevious() {
        seedUser(PHONE, PASSWORD);
        run(repo.resetPassword(PHONE, "newpass123")).assertComplete();
        // 原密码现在是「上一次使用的密码」
        assertBusiness(run(repo.resetPassword(PHONE, PASSWORD)),
                UserRepository.Errors.SAME_AS_RECENT, "新密码不能与最近使用过的密码相同");
        assertBusiness(run(repo.resetPassword(PHONE, "newpass123")),
                UserRepository.Errors.SAME_AS_RECENT, null);
        // 更早的密码可以再用
        run(repo.resetPassword(PHONE, "third1234")).assertComplete();
        run(repo.resetPassword(PHONE, PASSWORD)).assertComplete();
    }

    @Test
    public void resetPassword_success_updatesHashUnlocksAndLogsOut() {
        UserEntity u = seedUser(PHONE, PASSWORD);
        u.failedAttempts = 0;
        u.lockedUntil = now + 5 * MINUTE;
        userDao.seed(u);
        run(repo.loginWithSms(PHONE, CODE)).assertComplete();
        assertEquals(u.id, session.userId());

        run(repo.resetPassword(PHONE, "newpass123")).assertComplete();

        UserEntity row = userDao.rows.get(u.id);
        assertTrue(PasswordHasher.verify("newpass123", row.salt, row.passwordHash));
        assertTrue(PasswordHasher.verify(PASSWORD, row.prevSalt, row.prevPasswordHash));
        assertEquals(0, row.lockedUntil);
        assertEquals(0, row.failedAttempts);
        assertEquals(0, session.userId());
        assertNull(session.token);
        assertEquals(1, session.clearCount);

        assertBusiness(run(repo.loginWithPassword(PHONE, PASSWORD)), UserRepository.Errors.WRONG_PASSWORD, null);
        run(repo.loginWithPassword(PHONE, "newpass123")).assertComplete();
    }

    @Test
    public void resetPassword_accountWithoutPassword_setsFirstPassword() {
        UserEntity u = seedUser(PHONE, null);
        run(repo.resetPassword(PHONE, "newpass123")).assertComplete();
        UserEntity row = userDao.rows.get(u.id);
        assertTrue(PasswordHasher.verify("newpass123", row.salt, row.passwordHash));
        assertNull(row.prevPasswordHash);
    }

    @Test
    public void resetPassword_notRegistered() {
        assertBusiness(run(repo.resetPassword(PHONE, "newpass123")),
                UserRepository.Errors.NOT_REGISTERED, "该手机号未注册");
        assertEquals(0, session.clearCount);
    }

    @Test
    public void resetPassword_weakPassword() {
        seedUser(PHONE, PASSWORD);
        assertBusiness(run(repo.resetPassword(PHONE, "short1")), "WEAK_PASSWORD", null);
    }

    // —— 申诉 ——

    private static AppealEntity appeal(String oldPhone, String newPhone) {
        AppealEntity a = new AppealEntity();
        a.oldPhone = oldPhone;
        a.newPhone = newPhone;
        a.nickname = "老张";
        a.lastLogin = "一个月内";
        a.region = "江苏 南京";
        return a;
    }

    @Test
    public void submitAppeal_samePhone() {
        assertBusiness(run(repo.submitAppeal(appeal(PHONE, PHONE))),
                UserRepository.Errors.SAME_PHONE, "新手机号不能与原手机号相同");
        assertTrue(appealDao.rows.isEmpty());
    }

    @Test
    public void submitAppeal_savesPendingRecordWithNumber() {
        TestObserver<String> t = run(repo.submitAppeal(appeal(PHONE, "13900001234")));
        t.assertComplete();
        String no = t.values().get(0);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(now));
        assertTrue(no, no.matches("HB" + today + "-\\d{4}"));

        assertEquals(1, appealDao.rows.size());
        AppealEntity saved = appealDao.rows.get(0);
        assertEquals(no, saved.appealNo);
        assertEquals(AppealEntity.STATUS_PENDING, saved.status);
        assertEquals(now, saved.createdAt);
        assertEquals(PHONE, saved.oldPhone);
        assertEquals("13900001234", saved.newPhone);
        assertEquals(1, saved.id);
    }

    @Test
    public void appealNo_format() {
        for (int i = 0; i < 50; i++) {
            assertTrue(repo.newAppealNo(now).matches("HB\\d{8}-\\d{4}"));
        }
    }

    // —— 登录态 ——

    @Test
    public void logout_clearsSession() {
        seedUser(PHONE, PASSWORD);
        run(repo.loginWithPassword(PHONE, PASSWORD)).assertComplete();
        assertTrue(session.userId() > 0);

        run(repo.logout()).assertComplete();
        assertEquals(0, session.userId());
        assertNull(session.token);
        assertFalse(session.guest);
        assertTrue(session.onboardingDone);   // 退出登录不重新显示引导页
        assertEquals(1, userDao.rows.size()); // 本机数据保留
    }

    @Test
    public void enterGuest_and_completeOnboarding() {
        run(repo.completeOnboarding()).assertComplete();
        assertTrue(session.onboardingDone);
        assertFalse(session.guest);

        session.onboardingDone = false;
        run(repo.enterGuest()).assertComplete();
        assertTrue(session.guest);
        assertTrue(session.onboardingDone);
    }

    @Test
    public void currentUser_followsSession() {
        UserEntity seeded = seedUser(PHONE, PASSWORD);
        TestSubscriber<UserEntity> sub = repo.currentUser().test();
        sub.assertNoValues();   // 未登录不发射

        run(repo.loginWithPassword(PHONE, PASSWORD)).assertComplete();
        assertTrue(sub.values().size() >= 1);
        assertEquals(seeded.id, sub.values().get(sub.values().size() - 1).id);

        run(repo.logout()).assertComplete();
        int count = sub.values().size();
        userDao.seed(userDao.rows.get(seeded.id));   // 退出后数据变化不再发射
        assertEquals(count, sub.values().size());
        sub.assertNotComplete();
        sub.assertNoErrors();
    }

    @Test
    public void isFakeSms_delegatesToSmsService() {
        assertTrue(repo.isFakeSms());
    }
}
