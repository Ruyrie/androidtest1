package com.heban.app.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.R;
import com.heban.app.data.model.TaskItem;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.TaskRepository;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.data.repository.WeatherRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.function.LongSupplier;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;

/**
 * H01 首页（技术准备文档 12.2）：Flowable.combineLatest 合并天气、今日农事、我的作物、未读消息数生成 HomeState；
 * 问候语按时间段变化；下拉刷新强制刷新天气；勾选农事写入 done。
 */
public class HomeViewModel extends BaseViewModel {
    public final MutableLiveData<HomeState> state = new MutableLiveData<>(HomeState.initial());
    /** 当前账号昵称；游客 / 未登录为 null（界面显示「农友」） */
    public final MutableLiveData<String> nickname = new MutableLiveData<>(null);
    /** 头像圆圈里的昵称首字；游客为 null */
    public final MutableLiveData<String> avatarText = new MutableLiveData<>(null);
    /** 问候语字符串资源（上午好 / 中午好 / 下午好 / 晚上好，%1$s 为昵称） */
    public final MutableLiveData<Integer> greetingRes = new MutableLiveData<>();
    /** 顶部日期所用的当前时间（毫秒） */
    public final MutableLiveData<Long> now = new MutableLiveData<>();
    public final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);

    private final WeatherRepository weatherRepo;
    private final TaskRepository taskRepo;
    private final CropRepository cropRepo;
    private final UserRepository userRepo;
    private final Flowable<Long> userIds;
    private final LongSupplier clock;

    public HomeViewModel(AppContainer c) {
        this(c, c.weatherRepository, c.taskRepository, c.cropRepository, c.userRepository,
                c.currentUser.observeUserId(), WeatherFormat::currentTimeMillis);
    }

    @VisibleForTesting
    HomeViewModel(AppContainer c, WeatherRepository weatherRepo, TaskRepository taskRepo,
                  CropRepository cropRepo, UserRepository userRepo, Flowable<Long> userIds,
                  LongSupplier clock) {
        super(c);
        this.weatherRepo = weatherRepo;
        this.taskRepo = taskRepo;
        this.cropRepo = cropRepo;
        this.userRepo = userRepo;
        this.userIds = userIds;
        this.clock = clock;
        updateClock();
        observeHome();
        observeUser();
    }

    private void observeHome() {
        add(Flowable.combineLatest(
                        weatherRepo.observe().onErrorReturn(WeatherStateUi::failed),
                        taskRepo.observeToday().onErrorReturnItem(Collections.emptyList()),
                        cropRepo.observeCropCards(null).onErrorReturnItem(Collections.emptyList()),
                        unreadMessages().onErrorReturnItem(0),
                        HomeState::new)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state::setValue, e -> toast.setValue(AppError.from(e).message)));
    }

    private void observeUser() {
        add(userIds.distinctUntilChanged()
                .switchMap(id -> id > 0
                        ? userRepo.currentUser().map(u -> u.nickname == null ? "" : u.nickname.trim())
                        : Flowable.just(""))
                .onErrorReturnItem("")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(name -> {
                    nickname.setValue(name.isEmpty() ? null : name);
                    avatarText.setValue(firstChar(name));
                }, e -> { }));
    }

    /**
     * 未读消息数（铃铛红点）。消息模块（S08）还没有数据来源，暂时固定为 0；
     * 接入消息 Repository 后在这里返回它的未读数流即可。
     */
    protected Flowable<Integer> unreadMessages() {
        return Flowable.just(0);
    }

    /** 页面回到前台时更新问候语和日期 */
    public void updateClock() {
        long t = clock.getAsLong();
        now.setValue(t);
        greetingRes.setValue(greetingRes(Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).getHour()));
    }

    /** 下拉刷新：忽略 30 分钟缓存，强制刷新天气（农事、作物来自本地，自动更新） */
    public void refresh() {
        refreshing.setValue(true);
        add(weatherRepo.refresh(true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> refreshing.setValue(false), e -> {
                    refreshing.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    /** 勾选完成 / 取消完成（调用前 Fragment 已检查登录） */
    public void setDone(@NonNull TaskItem task, boolean done) {
        add(taskRepo.setDone(task.id, done)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> { }, e -> toast.setValue(AppError.from(e).message)));
    }

    /** 问候语时间段：5–11 点上午好，11–13 点中午好，13–18 点下午好，其余晚上好 */
    @StringRes
    static int greetingRes(int hour) {
        if (hour >= 5 && hour < 11) return R.string.home_greeting_morning;
        if (hour >= 11 && hour < 13) return R.string.home_greeting_noon;
        if (hour >= 13 && hour < 18) return R.string.home_greeting_afternoon;
        return R.string.home_greeting_evening;
    }

    @Nullable
    static String firstChar(@Nullable String name) {
        if (name == null || name.isEmpty()) return null;
        return new String(Character.toChars(name.codePointAt(0)));
    }
}
