package com.heban.app.data.repository;

import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.LedgerDay;
import com.heban.app.data.model.LedgerMonthSummary;
import com.heban.app.data.model.MonthBalance;

import java.time.YearMonth;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 收支记账（D10 / D11；C04 可同步生成）。按当前账号隔离，金额单位为分。 */
public interface LedgerRepository {
    String[] EXPENSE_CATEGORIES = {"化肥", "农药", "种子种苗", "人工", "水电灌溉", "农机", "地租", "其他"};
    String[] INCOME_CATEGORIES = {"农产品销售", "政府补贴", "代耕作业", "其他"};

    /** 新增或更新（id != 0）；新增手动记账时按 PointsRules「记一笔账」奖励积分（每天 1 次） */
    Single<Long> addEntry(LedgerEntryEntity entry);

    Completable deleteEntry(long entryId);

    Single<LedgerEntryEntity> getEntry(long entryId);

    /** D10 顶部本月收入 / 支出 / 结余 */
    Flowable<LedgerMonthSummary> observeMonth(YearMonth month);

    /** D10 结余趋势：截至 endMonth 的最近 months 个月（含空月份，旧 → 新） */
    Flowable<List<MonthBalance>> observeRecentBalances(YearMonth endMonth, int months);

    /** D10 支出构成：按金额倒序，前 3 类之外合并为「其他」 */
    Flowable<List<CategoryShare>> observeExpenseMix(YearMonth month);

    /** D10 本月流水按天分组（新 → 旧） */
    Flowable<List<LedgerDay>> observeDays(YearMonth month);
}
