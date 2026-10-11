package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.dao.PointsDao;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.CheckinEntity;
import com.heban.app.data.local.entity.PointsTxnEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.CheckInState;
import com.heban.app.data.model.PointsSummary;
import com.heban.app.data.remote.BusinessException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * PointsRepository 的本地实现（points_txn / checkin）。余额与成长值都由流水求和，只追加不修改；
 * 「今天」按本地时区的自然日计算。查询随当前账号切换（未登录 / 游客为 0 值），写操作要求已登录，都在 io 线程执行。
 */
public final class LocalPointsRepository implements PointsRepository {
    private final AppDatabase db;
    private final CurrentUser user;
    /** 测试时注入的固定时钟；为 null 时使用系统时钟和当前时区 */
    @Nullable
    private final Clock fixedClock;

    public LocalPointsRepository(AppDatabase db, CurrentUser user) {
        this(db, user, null);
    }

    /** 测试用：指定时钟（决定「今天」和流水时间） */
    public LocalPointsRepository(AppDatabase db, CurrentUser user, @Nullable Clock clock) {
        this.db = db;
        this.user = user;
        this.fixedClock = clock;
    }

    private Clock clock() {
        return fixedClock != null ? fixedClock : Clock.systemDefaultZone();
    }

    private PointsDao dao() {
        return db.pointsDao();
    }

    @Override
    public Flowable<PointsSummary> observeSummary() {
        return UserScope.observe(user, () -> PointsCalculator.summary(0, 0, 0, 0), uid -> {
            Clock clock = clock();
            long[] month = DateRanges.month(YearMonth.now(clock), clock.getZone());
            return dao().observeTotals(uid, month[0], month[1]).map(PointsCalculator::summary);
        });
    }

    @Override
    public Flowable<Integer> observeBalance() {
        return UserScope.observe(user, () -> 0, uid -> dao().observeBalance(uid)).distinctUntilChanged();
    }

    /** 以今天结尾的 7 天签到格子；查询多取一天，跨过零点后仍能算出昨天的连续天数 */
    @Override
    public Flowable<CheckInState> observeCheckIn() {
        return UserScope.observe(user,
                () -> PointsCalculator.checkInState(Collections.emptyList(), LocalDate.now(clock())),
                uid -> {
                    LocalDate from = LocalDate.now(clock()).minusDays(PointsRules.STREAK_DAYS);
                    return dao().observeCheckinsSince(uid, from.toString())
                            .map(rows -> PointsCalculator.checkInState(rows, LocalDate.now(clock())));
                });
    }

    /**
     * 同一事务内：检查今天是否已签到 → 接着昨天计算连续天数 → 写入「每日签到 · 连续第 N 天」流水，
     * 第 7 天另写一条「连续签到满 7 天」流水 → 写入 checkin（points 为两者合计）。返回获得的积分。
     */
    @Override
    public Single<Integer> checkIn() {
        return UserScope.require(user).map(uid -> db.runInTransaction(() -> {
            Clock clock = clock();
            long now = clock.millis();
            LocalDate today = LocalDate.now(clock);
            CheckinEntity todayRow = dao().getCheckinSync(uid, today.toString());
            CheckinEntity yesterday = dao().getCheckinSync(uid, today.minusDays(1).toString());
            int earned = earnedToday(uid, today, clock.getZone());
            PointsCalculator.CheckInPlan plan = PointsCalculator.planCheckIn(todayRow != null, yesterday, earned);

            String ref = today.toString();
            insertTxn(uid, PointsRules.TYPE_CHECKIN, PointsCalculator.checkInTitle(plan.streak),
                    plan.daily.points, plan.daily.growth, ref, now);
            if (plan.bonus != null) {
                insertTxn(uid, PointsRules.TYPE_CHECKIN_STREAK, PointsCalculator.STREAK_BONUS_TITLE,
                        plan.bonus.points, plan.bonus.growth, ref, now);
            }
            CheckinEntity row = new CheckinEntity();
            row.userId = uid;
            row.date = today.toString();
            row.streak = plan.streak;
            row.points = plan.totalPoints();
            dao().insertCheckinSync(row);
            return plan.totalPoints();
        })).subscribeOn(Schedulers.io());
    }

    /**
     * 同一事务内统计今天这类流水的条数和今天任务已得积分：达到每日次数时不写流水、返回 0；
     * 否则积分最多补到每日 100 上限（成长值照常），写入流水并返回实际获得的积分。
     */
    @Override
    public Single<Integer> award(String type, String title, @Nullable String refId) {
        return UserScope.require(user).map(uid -> db.runInTransaction(() -> {
            Clock clock = clock();
            LocalDate today = LocalDate.now(clock);
            long[] day = DateRanges.day(today, clock.getZone());
            int count = dao().countBetweenSync(uid, type, day[0], day[1]);
            PointsCalculator.Reward reward = PointsCalculator.award(type, count, earnedToday(uid, today, clock.getZone()));
            if (reward == null) return 0;
            insertTxn(uid, type, title, reward.points, reward.growth, refId, clock.millis());
            return reward.points;
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable grant(String type, String title, int points, int growth, @Nullable String refId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() -> {
            if (points < 0 || growth < 0) throw new IllegalArgumentException("grant expects non-negative values");
            insertTxn(uid, type, title, points, growth, refId, clock().millis());
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable spend(String type, String title, int points, @Nullable String refId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                spendInTransaction(uid, type, title, points, refId))).subscribeOn(Schedulers.io());
    }

    /** 检查余额后追加一条负数流水；已在外层事务中时并入外层事务 */
    @Override
    public void spendInTransaction(long userId, String type, String title, int points, @Nullable String refId) {
        if (userId <= 0) throw UserScope.notLoggedIn();
        if (points <= 0) throw new IllegalArgumentException("spend expects a positive amount");
        db.runInTransaction(() -> {
            if (dao().balanceSync(userId) < points) {
                throw new BusinessException(PointsCalculator.POINTS_NOT_ENOUGH, "积分不足");
            }
            insertTxn(userId, type, title, -points, 0, refId, clock().millis());
        });
    }

    /** 近 3 个月（从 3 个月前的同一天 0 点起）的流水，新 → 旧 */
    @Override
    public Flowable<List<PointsTxnEntity>> observeTxns(String filter) {
        String f = PointsCalculator.normalizeFilter(filter);
        return UserScope.observe(user, Collections::<PointsTxnEntity>emptyList, uid -> {
            Clock clock = clock();
            long since = DateRanges.startOfDay(LocalDate.now(clock).minusMonths(3), clock.getZone());
            switch (f) {
                case PointsCalculator.FILTER_GAIN:
                    return dao().observeGainTxnsSince(uid, since);
                case PointsCalculator.FILTER_USE:
                    return dao().observeUseTxnsSince(uid, since);
                default:
                    return dao().observeTxnsSince(uid, since);
            }
        });
    }

    @Override
    public Flowable<Integer> observeTodayCount(String type) {
        return UserScope.observe(user, () -> 0, uid -> {
            Clock clock = clock();
            long[] day = DateRanges.day(LocalDate.now(clock), clock.getZone());
            return dao().observeCountBetween(uid, type, day[0], day[1]);
        }).distinctUntilChanged();
    }

    /** 今天通过任务（签到、发布、回答、农事、记账）已获得的积分 */
    private int earnedToday(long uid, LocalDate today, ZoneId zone) {
        long[] day = DateRanges.day(today, zone);
        return dao().sumGainBetweenSync(uid, PointsCalculator.CAPPED_TYPES, day[0], day[1]);
    }

    private void insertTxn(long uid, String type, String title, int amount, int growth,
                           @Nullable String refId, long createdAt) {
        PointsTxnEntity t = new PointsTxnEntity();
        t.userId = uid;
        t.type = type;
        t.title = title;
        t.amount = amount;
        t.growth = growth;
        t.refId = refId;
        t.createdAt = createdAt;
        dao().insertTxnSync(t);
    }
}
