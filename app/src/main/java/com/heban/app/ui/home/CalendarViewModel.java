package com.heban.app.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.heban.app.R;
import com.heban.app.data.model.TaskItem;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.TaskRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.SerialDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * H03 农事日历：当前月份、所选日期、月历格子（今天 / 选中 / 节气 / 农事圆点）、所选日期的农事与节气提示。
 * 切换月份时选中该月的今天（不在该月则选 1 号），保证月历与下方列表一致。
 */
public class CalendarViewModel extends BaseViewModel {
    private final TaskRepository tasks;
    private final Clock clock;

    /** 正在显示的月份 */
    public final MutableLiveData<YearMonth> month = new MutableLiveData<>();
    /** 选中的日期（下方显示这一天的农事） */
    public final MutableLiveData<LocalDate> selected = new MutableLiveData<>();
    /** 月历格子（含月初空格） */
    public final MutableLiveData<List<CalendarDay>> days = new MutableLiveData<>(Collections.emptyList());
    /** 选中日期的农事，按时间排序；加载中为 null */
    public final MutableLiveData<List<TaskItem>> dayTasks = new MutableLiveData<>();
    /** 选中日期当天或之后最近的节气（用于节气提示），超出数据范围为 null */
    public final MutableLiveData<SolarTerms.Term> upcomingTerm = new MutableLiveData<>();
    /** 选中日期没有农事（加载完成后才为 true，避免切换日期时闪现空状态） */
    public final LiveData<Boolean> empty = Transformations.map(dayTasks, list -> list != null && list.isEmpty());
    /** 打开 H04 新建农事，参数为选中的日期 */
    public final SingleLiveEvent<LocalDate> openAddTask = new SingleLiveEvent<>();
    /** 资源文字的一次性提示 */
    public final SingleLiveEvent<Integer> toastRes = new SingleLiveEvent<>();

    private final SerialDisposable monthSub = new SerialDisposable();
    private final SerialDisposable daySub = new SerialDisposable();
    private SolarTerms terms = SolarTerms.EMPTY;
    private Map<LocalDate, Set<String>> monthTypes = Collections.emptyMap();
    private boolean started;

    public CalendarViewModel(AppContainer c) {
        this(c, c.taskRepository, Single.fromCallable(() -> SolarTerms.load(c.app)), Clock.systemDefaultZone());
    }

    CalendarViewModel(@Nullable AppContainer c, @NonNull TaskRepository tasks,
                      @NonNull Single<SolarTerms> termsSource, @NonNull Clock clock) {
        super(c);
        this.tasks = tasks;
        this.clock = clock;
        add(monthSub);
        add(daySub);
        add(termsSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(t -> {
                    terms = t;
                    updateTerm();
                    rebuildDays();
                }, e -> { /* 节气表读不到时只是不显示节气 */ }));
    }

    @NonNull
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** 首次进入页面：打开参数中的日期，缺省为今天；重建视图时不重复执行 */
    public void start(@Nullable LocalDate initial) {
        if (started) return;
        started = true;
        select(initial != null ? initial : today());
    }

    public void select(@Nullable LocalDate day) {
        if (day == null) return;
        YearMonth ym = YearMonth.from(day);
        boolean monthChanged = !ym.equals(month.getValue());
        boolean dayChanged = !day.equals(selected.getValue());
        selected.setValue(day);
        if (monthChanged) {
            month.setValue(ym);
            monthTypes = Collections.emptyMap();
            subscribeMonth(ym);
        }
        if (dayChanged) subscribeDay(day);
        updateTerm();
        rebuildDays();
    }

    public void onDayClicked(@Nullable CalendarDay day) {
        if (day != null && day.date != null) select(day.date);
    }

    public void previousMonth() {
        YearMonth m = month.getValue();
        if (m != null) showMonth(m.minusMonths(1));
    }

    public void nextMonth() {
        YearMonth m = month.getValue();
        if (m != null) showMonth(m.plusMonths(1));
    }

    /** 跳到某个月：该月包含今天时选中今天，否则选中 1 号 */
    public void showMonth(@NonNull YearMonth ym) {
        LocalDate t = today();
        select(YearMonth.from(t).equals(ym) ? t : ym.atDay(1));
    }

    /** 顶栏「回到今天」 */
    public void goToday() {
        select(today());
    }

    /** FAB「新建农事」 */
    public void addTask() {
        LocalDate day = selected.getValue();
        openAddTask.setValue(day != null ? day : today());
    }

    /** 勾选完成 / 取消完成 */
    public void toggleDone(@NonNull TaskItem task) {
        add(tasks.setDone(task.id, !task.done)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> { }, this::onError));
    }

    public void delete(@NonNull TaskItem task) {
        add(tasks.deleteTask(task.id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> toast(R.string.calendar_deleted), this::onError));
    }

    // ---------- 内部 ----------

    private void subscribeMonth(YearMonth ym) {
        monthSub.set(tasks.observeMonthTypes(ym)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(map -> {
                    if (!ym.equals(month.getValue())) return;
                    monthTypes = map == null ? Collections.emptyMap() : map;
                    rebuildDays();
                }, this::onError));
    }

    private void subscribeDay(LocalDate day) {
        dayTasks.setValue(null);
        daySub.set(tasks.observeTasksOn(day)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    if (!day.equals(selected.getValue())) return;
                    dayTasks.setValue(list == null ? Collections.emptyList() : list);
                }, e -> {
                    if (day.equals(selected.getValue())) dayTasks.setValue(Collections.emptyList());
                    onError(e);
                }));
    }

    private void updateTerm() {
        LocalDate day = selected.getValue();
        upcomingTerm.setValue(day == null ? null : terms.nextOnOrAfter(day));
    }

    private void rebuildDays() {
        YearMonth ym = month.getValue();
        if (ym == null) return;
        LocalDate today = today();
        LocalDate sel = selected.getValue();
        List<CalendarDay> out = new ArrayList<>();
        for (LocalDate d : CalendarDates.cells(ym)) {
            if (d == null) {
                out.add(CalendarDay.blank());
                continue;
            }
            Set<String> types = monthTypes.get(d);
            out.add(new CalendarDay(d, d.equals(today), d.equals(sel), terms.nameOn(d),
                    CalendarTypeStyle.MANAGE.presentIn(types),
                    CalendarTypeStyle.IRRIGATE.presentIn(types),
                    CalendarTypeStyle.PEST.presentIn(types)));
        }
        days.setValue(out);
    }

    private void toast(@StringRes int res) {
        toastRes.setValue(res);
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }
}
