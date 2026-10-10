package com.heban.app.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.IdRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.datastore.preferences.core.Preferences;

import com.heban.app.R;
import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.WeatherAlert;
import com.heban.app.ui.MainActivity;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import io.reactivex.rxjava3.core.Completable;

/**
 * 通知渠道与发送（技术准备文档 11.2）。发送前检查 M09 中的开关和免打扰时段（天气预警不受免打扰限制）；
 * 点击通知打开 MainActivity 并跳到对应页面（EXTRA_OPEN_DEST）。
 */
public final class Notifier {
    public static final String CH_FARM_TASK = "farm_task";
    public static final String CH_WEATHER = "weather_alert";
    public static final String CH_PEST = "pest_alert";
    public static final String CH_INTERACTION = "interaction";
    public static final String CH_DM = "direct_message";
    public static final String CH_ORDER = "order";

    private static final int ID_WEATHER = 1;
    private static final int ID_TASK_BASE = 100_000;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final Context app;
    private final SettingsStore settings;

    public Notifier(Context app, SettingsStore settings) {
        this.app = app;
        this.settings = settings;
    }

    public void createChannels() {
        NotificationManager nm = app.getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.createNotificationChannel(channel(CH_FARM_TASK, R.string.channel_farm_task,
                NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(channel(CH_WEATHER, R.string.channel_weather,
                NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(channel(CH_PEST, R.string.channel_pest,
                NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(channel(CH_INTERACTION, R.string.channel_interaction,
                NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(channel(CH_DM, R.string.channel_dm,
                NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(channel(CH_ORDER, R.string.channel_order,
                NotificationManager.IMPORTANCE_DEFAULT));
    }

    private NotificationChannel channel(String id, int nameRes, int importance) {
        return new NotificationChannel(id, app.getString(nameRes), importance);
    }

    /** 农事任务提醒：标题「农事提醒」，内容「15:00 给番茄追施钾肥」，点击打开 H03 */
    public Completable showTaskReminder(FarmTaskEntity task) {
        return Completable.fromAction(() -> {
            if (!allowed(SettingsStore.NOTIFY_FARM_TASK, true, true)) return;
            String time = Instant.ofEpochMilli(task.dueAt).atZone(ZoneId.systemDefault())
                    .toLocalTime().format(HM);
            post(ID_TASK_BASE + (int) (task.id % 1_000_000), CH_FARM_TASK,
                    app.getString(R.string.channel_farm_task), time + " " + task.title,
                    R.drawable.ic_task_alt, R.id.calendarFragment);
        });
    }

    /** 天气预警（免打扰时段也提醒），点击打开 H02 */
    public Completable showWeatherAlert(WeatherAlert alert) {
        return Completable.fromAction(() -> {
            if (!allowed(SettingsStore.NOTIFY_WEATHER, true, false)) return;
            post(ID_WEATHER, CH_WEATHER, alert.title, alert.detailText,
                    R.drawable.ic_warning, R.id.weatherFragment);
        });
    }

    private boolean allowed(Preferences.Key<Boolean> key,
                            boolean defaultOn, boolean respectDnd) {
        if (!settings.peek(SettingsStore.NOTIFY_ENABLED, true)) return false;
        if (!settings.peek(key, defaultOn)) return false;
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;
        return !respectDnd || !inDnd(LocalTime.now());
    }

    /** 是否处于免打扰时段（跨午夜时 start > end） */
    boolean inDnd(LocalTime now) {
        int start = settings.peek(SettingsStore.NOTIFY_DND_START, SettingsStore.DEFAULT_DND_START);
        int end = settings.peek(SettingsStore.NOTIFY_DND_END, SettingsStore.DEFAULT_DND_END);
        return inWindow(now.getHour() * 60 + now.getMinute(), start, end);
    }

    static boolean inWindow(int minute, int start, int end) {
        if (start == end) return false;
        return start < end ? minute >= start && minute < end : minute >= start || minute < end;
    }

    private void post(int id, String channel, String title, String text, int icon, @IdRes int dest) {
        Intent intent = new Intent(app, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_DEST, dest);
        PendingIntent pi = PendingIntent.getActivity(app, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(app, channel)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(app).notify(id, b.build());
        } catch (SecurityException ignored) {
            // 通知权限在检查后被撤销
        }
    }
}
