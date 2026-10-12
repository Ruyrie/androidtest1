package com.heban.app.ui.discover.ledger;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.R;
import com.heban.app.data.model.LedgerMonthSummary;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.LedgerRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * D10 收支记账（技术准备文档 12.4）：按月统计（SUM … GROUP BY 由 Repository 完成）、近 6 个月结余柱状图、
 * 支出构成、按天分组的流水。月份可以向前切换，不能超过当前月。
 */
public class LedgerViewModel extends BaseViewModel {
    /** 结余趋势显示的月份数 */
    public static final int TREND_MONTHS = 6;

    public final MutableLiveData<YearMonth> month = new MutableLiveData<>();
    public final MutableLiveData<String> monthTitle = new MutableLiveData<>("");
    /** 是否可以切到下个月（不能超过当前月） */
    public final MutableLiveData<Boolean> canNext = new MutableLiveData<>(false);
    /** 正在看的是不是当前月（「本月结余」/「9月结余」） */
    public final MutableLiveData<Boolean> isCurrentMonth = new MutableLiveData<>(true);

    public final MutableLiveData<String> balanceText = new MutableLiveData<>(LedgerFormat.signedMoney(0));
    public final MutableLiveData<String> incomeText = new MutableLiveData<>(LedgerFormat.money(0));
    public final MutableLiveData<String> expenseText = new MutableLiveData<>(LedgerFormat.money(0));

    public final MutableLiveData<List<LedgerChartModel.Bar>> bars = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<List<LedgerChartModel.MixRow>> mix = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<List<LedgerRow>> rows = new MutableLiveData<>(Collections.emptyList());
    /** 本月没有任何流水（流水加载完成后才为 true） */
    public final MutableLiveData<Boolean> empty = new MutableLiveData<>(false);

    /** false：结余趋势；true：支出构成 */
    public final MutableLiveData<Boolean> showMix = new MutableLiveData<>(false);

    public final SingleLiveEvent<Integer> messageRes = new SingleLiveEvent<>();

    private final LedgerRepository repo;
    private final Clock clock;
    private final CompositeDisposable monthSubs = new CompositeDisposable();
    private boolean started;

    public LedgerViewModel(AppContainer c) {
        this(c, c.ledgerRepository, Clock.systemDefaultZone());
    }

    @VisibleForTesting
    LedgerViewModel(AppContainer c, LedgerRepository repo, Clock clock) {
        super(c);
        this.repo = repo;
        this.clock = clock;
        add(monthSubs);
    }

    /** 第一次进入时显示当前月 */
    public void start() {
        if (started) return;
        started = true;
        load(currentMonth());
    }

    @NonNull
    public YearMonth currentMonth() {
        return YearMonth.now(clock);
    }

    @NonNull
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public void previousMonth() {
        YearMonth m = month.getValue();
        load((m == null ? currentMonth() : m).minusMonths(1));
    }

    public void nextMonth() {
        YearMonth m = month.getValue();
        if (m == null || !m.isBefore(currentMonth())) return;
        load(m.plusMonths(1));
    }

    public void showTrend() {
        if (Boolean.TRUE.equals(showMix.getValue())) showMix.setValue(false);
    }

    public void showMix() {
        if (!Boolean.TRUE.equals(showMix.getValue())) showMix.setValue(true);
    }

    /** 删除一笔手动记账（来自农事记录的条目由页面拦截） */
    public void deleteEntry(long entryId) {
        add(repo.deleteEntry(entryId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> messageRes.setValue(R.string.ledger_deleted),
                        e -> toast.setValue(AppError.from(e).message)));
    }

    private void load(@NonNull YearMonth m) {
        YearMonth now = currentMonth();
        YearMonth target = m.isAfter(now) ? now : m;
        month.setValue(target);
        monthTitle.setValue(LedgerFormat.monthTitle(target));
        canNext.setValue(target.isBefore(now));
        isCurrentMonth.setValue(target.equals(now));

        monthSubs.clear();
        empty.setValue(false);
        rows.setValue(Collections.emptyList());
        onSummary(new LedgerMonthSummary());
        mix.setValue(Collections.emptyList());

        monthSubs.add(repo.observeMonth(target)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSummary, this::onError));
        monthSubs.add(repo.observeRecentBalances(target, TREND_MONTHS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> bars.setValue(LedgerChartModel.bars(list)), this::onError));
        monthSubs.add(repo.observeExpenseMix(target)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> mix.setValue(LedgerChartModel.mixRows(list)), this::onError));
        monthSubs.add(repo.observeDays(target)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(days -> {
                    List<LedgerRow> list = LedgerRow.from(days, today());
                    rows.setValue(list);
                    empty.setValue(list.isEmpty());
                }, e -> {
                    empty.setValue(true);
                    onError(e);
                }));
    }

    private void onSummary(LedgerMonthSummary s) {
        LedgerMonthSummary v = s == null ? new LedgerMonthSummary() : s;
        balanceText.setValue(LedgerFormat.signedMoney(v.balanceCent()));
        incomeText.setValue(LedgerFormat.money(v.incomeCent));
        expenseText.setValue(LedgerFormat.money(v.expenseCent));
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }
}
