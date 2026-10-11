package com.heban.app.data.repository;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.LedgerDay;
import com.heban.app.data.model.LedgerMonthSummary;
import com.heban.app.data.model.MonthBalance;
import com.heban.app.data.remote.BusinessException;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * LedgerRepository 的本地实现（Room），按当前账号隔离；未登录时写操作报 NOT_LOGGED_IN。
 * 统计按本地时区的自然月：[1 日 0 点, 下月 1 日 0 点)，用 SUM(amount_cent) … GROUP BY 汇总。
 */
public final class LocalLedgerRepository implements LedgerRepository {
    /** 修改由农事记录同步生成的账目 */
    public static final String RECORD_ENTRY_READONLY = "RECORD_ENTRY_READONLY";
    /** 「记一笔账」积分流水标题 */
    static final String LEDGER_AWARD_TITLE = "记一笔账";

    private final AppDatabase db;
    private final CurrentUser user;
    /** 手动记一笔按 PointsRules「记一笔账」奖励积分 */
    private final PointsRepository points;

    public LocalLedgerRepository(AppDatabase db, CurrentUser user, PointsRepository points) {
        this.db = db;
        this.user = user;
        this.points = points;
    }

    private static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    /**
     * id == 0：新增一笔收支；未填日期时取当前时间，source 默认 manual。新增手动记账后按「记一笔账」奖励积分
     * （每天 1 次，积分出错不影响保存）。
     * id != 0：修改这一笔（保留账号、创建时间和来源）；只有手动记账可以修改，来自农事记录的账目报
     * RECORD_ENTRY_READONLY。
     */
    @Override
    public Single<Long> addEntry(LedgerEntryEntity entry) {
        if (entry.id != 0) return updateEntry(entry);
        return UserScope.require(user).map(uid -> {
            long now = System.currentTimeMillis();
            entry.id = 0;
            entry.userId = uid;
            entry.createdAt = now;
            if (entry.date == 0) entry.date = now;
            long id = db.ledgerDao().insertSync(entry);
            entry.id = id;
            return id;
        }).flatMap(id -> LedgerEntryEntity.SOURCE_MANUAL.equals(entry.source)
                ? points.award(PointsRules.TYPE_LEDGER, LEDGER_AWARD_TITLE, String.valueOf(id))
                .onErrorReturnItem(0).map(p -> id)
                : Single.just(id)
        ).subscribeOn(Schedulers.io());
    }

    private Single<Long> updateEntry(LedgerEntryEntity entry) {
        return UserScope.require(user).map(uid -> db.runInTransaction(() -> {
            LedgerEntryEntity old = db.ledgerDao().getSync(entry.id, uid);
            if (old == null) throw UserScope.notFound("账目不存在");
            if (!LedgerEntryEntity.SOURCE_MANUAL.equals(old.source)) {
                throw new BusinessException(RECORD_ENTRY_READONLY, "来自农事记录的账目请在农事记录中修改");
            }
            entry.userId = uid;
            entry.createdAt = old.createdAt;
            entry.source = old.source;
            entry.recordId = old.recordId;
            if (entry.date == 0) entry.date = old.date;
            db.ledgerDao().updateSync(entry);
            return entry.id;
        })).subscribeOn(Schedulers.io());
    }

    /** 删除一笔收支；若由农事记录同步生成，解除记录上的关联 */
    @Override
    public Completable deleteEntry(long entryId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                db.runInTransaction(() -> {
                    db.farmRecordDao().clearLedgerEntrySync(entryId, uid);
                    db.ledgerDao().deleteSync(entryId, uid);
                }))).subscribeOn(Schedulers.io());
    }

    /** 当前账号的一笔收支；不存在时报 NOT_FOUND */
    @Override
    public Single<LedgerEntryEntity> getEntry(long entryId) {
        return UserScope.require(user).map(uid -> {
            LedgerEntryEntity e = db.ledgerDao().getSync(entryId, uid);
            if (e == null) throw UserScope.notFound("账目不存在");
            return e;
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<LedgerMonthSummary> observeMonth(YearMonth month) {
        return UserScope.observe(user, LedgerMonthSummary::new, uid -> {
            long[] range = DateRanges.month(month, zone());
            return db.ledgerDao().observeTypeTotals(uid, range[0], range[1]).map(LedgerStats::monthSummary);
        }).distinctUntilChanged((a, b) -> a.incomeCent == b.incomeCent && a.expenseCent == b.expenseCent);
    }

    @Override
    public Flowable<List<MonthBalance>> observeRecentBalances(YearMonth endMonth, int months) {
        if (months <= 0) return Flowable.just(Collections.emptyList());
        return UserScope.observe(user, () -> LedgerStats.balances(endMonth, months, Collections.emptyList()),
                uid -> {
                    long[] bounds = LedgerStats.monthBounds(endMonth, months, zone());
                    return db.ledgerDao().observeBucketTotals(bucketQuery(uid, bounds))
                            .map(rows -> LedgerStats.balances(endMonth, months, rows));
                });
    }

    @Override
    public Flowable<List<CategoryShare>> observeExpenseMix(YearMonth month) {
        return UserScope.observe(user, Collections::<CategoryShare>emptyList, uid -> {
            long[] range = DateRanges.month(month, zone());
            return db.ledgerDao().observeExpenseByCategory(uid, range[0], range[1]).map(LedgerStats::expenseMix);
        });
    }

    @Override
    public Flowable<List<LedgerDay>> observeDays(YearMonth month) {
        return UserScope.observe(user, Collections::<LedgerDay>emptyList, uid -> {
            ZoneId zone = zone();
            long[] range = DateRanges.month(month, zone);
            return db.ledgerDao().observeRowsBetween(uid, range[0], range[1])
                    .map(rows -> LedgerStats.groupDays(rows, zone));
        });
    }

    /**
     * 结余趋势的分组查询：bucket 为条目所在月份的序号（0 为最早的月份），按 bucket、收支类型求和。
     * bounds 来自 LedgerStats.monthBounds（months + 1 个边界）。
     */
    static SupportSQLiteQuery bucketQuery(long userId, long[] bounds) {
        int months = bounds.length - 1;
        Object[] args = new Object[months + 2];
        StringBuilder sql = new StringBuilder("SELECT ");
        if (months == 1) {
            sql.append("0");
        } else {
            sql.append("CASE");
            for (int i = 1; i < months; i++) {
                sql.append(" WHEN date < ? THEN ").append(i - 1);
                args[i - 1] = bounds[i];
            }
            sql.append(" ELSE ").append(months - 1).append(" END");
        }
        sql.append(" AS bucket, type, SUM(amount_cent) AS total")
                .append(" FROM ledger_entry WHERE user_id = ? AND date >= ? AND date < ?")
                .append(" GROUP BY bucket, type");
        args[months - 1] = userId;
        args[months] = bounds[0];
        args[months + 1] = bounds[months];
        return new SimpleSQLiteQuery(sql.toString(), args);
    }
}
