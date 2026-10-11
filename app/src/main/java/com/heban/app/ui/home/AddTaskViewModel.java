package com.heban.app.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.TaskRepository;
import com.heban.app.data.repository.WeatherRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

/**
 * H04 新建农事：农事类型（默认用药）、关联作物、日期（默认明天）、时间（默认 08:00）、重复、提醒、备注；
 * 类型或日期变化时刷新「AI 提示」（WeatherRepository.farmTaskTip，空字符串时隐藏）。
 */
public class AddTaskViewModel extends BaseViewModel {
    /** 「提醒我」开启时提前的分钟数 */
    public static final int REMIND_MINUTES = 30;
    public static final LocalTime DEFAULT_TIME = LocalTime.of(8, 0);
    /** 重复选项：间隔天数（0 = 不重复）与次数 */
    public static final int[] REPEAT_EVERY_DAYS = {0, 1, 3, 7, 15};
    public static final int[] REPEAT_TIMES = {1, 3, 3, 3, 3};

    private final TaskRepository tasks;
    private final CropRepository crops;
    private final WeatherRepository weather;
    private final Clock clock;

    public final MutableLiveData<FarmType> type = new MutableLiveData<>(FarmType.SPRAY);
    /** 当前账号的作物（关联作物下拉） */
    public final MutableLiveData<List<CropCard>> cropOptions = new MutableLiveData<>(Collections.emptyList());
    /** 选中的作物，null 表示不关联 */
    public final MutableLiveData<CropCard> crop = new MutableLiveData<>();
    public final MutableLiveData<LocalDate> date = new MutableLiveData<>();
    public final MutableLiveData<LocalTime> time = new MutableLiveData<>(DEFAULT_TIME);
    /** REPEAT_* 的下标 */
    public final MutableLiveData<Integer> repeatIndex = new MutableLiveData<>(0);
    public final MutableLiveData<Boolean> remind = new MutableLiveData<>(true);
    public final MutableLiveData<String> note = new MutableLiveData<>("");
    /** AI 提示正文（不含「AI 提示：」），空字符串时隐藏提示卡 */
    public final MutableLiveData<String> tip = new MutableLiveData<>("");
    public final LiveData<Boolean> tipVisible = Transformations.map(tip, t -> t != null && !t.trim().isEmpty());
    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    /** 保存成功 */
    public final SingleLiveEvent<Long> saved = new SingleLiveEvent<>();

    private final SerialDisposable tipSub = new SerialDisposable();
    @Nullable private String presetTitle;
    private long presetCropId;
    private boolean cropChosen;
    private boolean started;

    public AddTaskViewModel(AppContainer c) {
        this(c, c.taskRepository, c.cropRepository, c.weatherRepository, Clock.systemDefaultZone());
    }

    AddTaskViewModel(@Nullable AppContainer c, @NonNull TaskRepository tasks, @NonNull CropRepository crops,
                     @NonNull WeatherRepository weather, @NonNull Clock clock) {
        super(c);
        this.tasks = tasks;
        this.crops = crops;
        this.weather = weather;
        this.clock = clock;
        add(tipSub);
    }

    /**
     * 首次进入：按参数预填。typeName 为 FarmType 名称；epochDay 缺省时为明天；cropId 0 表示未指定。
     */
    public void start(@Nullable String typeName, @Nullable Long epochDay, long cropId, @Nullable String title) {
        if (started) return;
        started = true;
        if (typeName != null && !typeName.isEmpty()) type.setValue(FarmType.of(typeName));
        LocalDate day = CalendarDates.fromEpochDay(epochDay);
        date.setValue(day != null ? day : LocalDate.now(clock).plusDays(1));
        presetCropId = cropId;
        presetTitle = title == null || title.trim().isEmpty() ? null : title.trim();
        add(crops.observeCropCards(null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCrops, e -> cropOptions.setValue(Collections.emptyList())));
        refreshTip();
    }

    private void onCrops(List<CropCard> list) {
        List<CropCard> cards = list == null ? Collections.emptyList() : list;
        cropOptions.setValue(cards);
        CropCard current = crop.getValue();
        if (!cropChosen) {
            // 默认：参数指定的作物，否则第一种作物（原型默认显示一种作物）
            CropCard pick = findCrop(cards, presetCropId);
            if (pick == null && presetCropId == 0 && !cards.isEmpty()) pick = cards.get(0);
            crop.setValue(pick);
        } else if (current != null) {
            // 作物被删除时改为不关联；否则换成最新的卡片数据
            crop.setValue(findCrop(cards, current.id));
        }
    }

    @Nullable
    private static CropCard findCrop(List<CropCard> cards, long id) {
        if (id == 0) return null;
        for (CropCard c : cards) {
            if (c.id == id) return c;
        }
        return null;
    }

    public void selectType(@NonNull FarmType t) {
        if (t == type.getValue()) return;
        type.setValue(t);
        refreshTip();
    }

    /** 选择关联作物；null 表示不关联 */
    public void selectCrop(@Nullable CropCard card) {
        cropChosen = true;
        crop.setValue(card);
    }

    public void selectDate(@NonNull LocalDate day) {
        if (day.equals(date.getValue())) return;
        date.setValue(day);
        refreshTip();
    }

    public void selectTime(int hour, int minute) {
        time.setValue(LocalTime.of(hour, minute));
    }

    public void selectRepeat(int index) {
        if (index >= 0 && index < REPEAT_EVERY_DAYS.length) repeatIndex.setValue(index);
    }

    public void setRemind(boolean on) {
        if (!Boolean.valueOf(on).equals(remind.getValue())) remind.setValue(on);
    }

    /** 任务标题：参数给定的标题，否则「用药 · 番茄」，不关联作物时为类型名 */
    @NonNull
    public String title() {
        if (presetTitle != null) return presetTitle;
        FarmType t = type.getValue() == null ? FarmType.OTHER : type.getValue();
        CropCard c = crop.getValue();
        return c == null || c.name == null || c.name.isEmpty() ? t.label : t.label + " · " + c.name;
    }

    /** 组装要保存的任务（user_id、series_id 由 Repository 填写） */
    @NonNull
    FarmTaskEntity buildTask() {
        FarmTaskEntity e = new FarmTaskEntity();
        FarmType t = type.getValue() == null ? FarmType.OTHER : type.getValue();
        e.type = t.name();
        e.title = title();
        CropCard c = crop.getValue();
        e.cropId = c == null ? null : c.id;
        LocalDate day = date.getValue() == null ? LocalDate.now(clock).plusDays(1) : date.getValue();
        LocalTime at = time.getValue() == null ? DEFAULT_TIME : time.getValue();
        e.dueAt = CalendarDates.toEpochMillis(day, at, clock.getZone());
        e.remindMinutes = Boolean.TRUE.equals(remind.getValue()) ? REMIND_MINUTES : -1;
        String n = note.getValue() == null ? "" : note.getValue().trim();
        e.note = n.isEmpty() ? null : n;
        e.createdAt = clock.millis();
        return e;
    }

    public void save() {
        if (Boolean.TRUE.equals(saving.getValue())) return;
        int idx = repeatIndex.getValue() == null ? 0 : repeatIndex.getValue();
        saving.setValue(true);
        add(tasks.saveTask(buildTask(), REPEAT_EVERY_DAYS[idx], REPEAT_TIMES[idx])
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id -> {
                    saving.setValue(false);
                    saved.setValue(id);
                }, e -> {
                    saving.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    private void refreshTip() {
        FarmType t = type.getValue();
        LocalDate day = date.getValue();
        if (t == null || day == null) return;
        tipSub.set(weather.farmTaskTip(t, day)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> tip.setValue(s == null ? "" : s), e -> tip.setValue("")));
    }
}
