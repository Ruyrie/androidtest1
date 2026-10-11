package com.heban.app.ui.home;

import androidx.annotation.NonNull;

import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.TaskItem;
import com.heban.app.data.model.WeatherState;

import java.util.Collections;
import java.util.List;

/** H01 首页状态：天气、今日农事、我的作物、未读消息数四个数据流合并而成（技术准备文档 12.2）。 */
public final class HomeState {
    @NonNull public final WeatherStateUi weather;
    @NonNull public final List<TaskItem> tasks;
    @NonNull public final List<CropCard> crops;
    public final int unread;
    /** 四个数据流都已发射过（之前不显示空状态，避免闪一下「还没有作物」） */
    public final boolean loaded;

    public HomeState(WeatherState weather, List<TaskItem> tasks, List<CropCard> crops, Integer unread) {
        this(weather, tasks, crops, unread, true);
    }

    private HomeState(WeatherState weather, List<TaskItem> tasks, List<CropCard> crops, Integer unread,
                      boolean loaded) {
        this.loaded = loaded;
        this.weather = new WeatherStateUi(weather);
        this.tasks = tasks != null ? tasks : Collections.emptyList();
        this.crops = crops != null ? crops : Collections.emptyList();
        this.unread = unread != null ? unread : 0;
    }

    public static HomeState initial() {
        return new HomeState(WeatherStateUi.loading(), null, null, 0, false);
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public int getDoneCount() {
        int n = 0;
        for (TaskItem t : tasks) {
            if (t.done) n++;
        }
        return n;
    }

    /** 显示「今天没有农事」空状态 */
    public boolean isTasksEmpty() {
        return loaded && tasks.isEmpty();
    }

    public boolean hasTasks() {
        return !tasks.isEmpty();
    }

    public int getCropCount() {
        return crops.size();
    }

    /** 显示「还没有作物」空状态 */
    public boolean isCropsEmpty() {
        return loaded && crops.isEmpty();
    }

    public boolean hasCrops() {
        return !crops.isEmpty();
    }

    /** 铃铛红点：有未读消息时显示（不计数） */
    public boolean isShowUnreadDot() {
        return unread > 0;
    }
}
