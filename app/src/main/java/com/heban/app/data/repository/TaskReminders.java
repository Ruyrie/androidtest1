package com.heban.app.data.repository;

import android.content.Context;
import android.util.Log;

import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.worker.FarmTaskReminderWorker;

/**
 * 安排 / 取消农事提醒。WorkManager 未初始化（单元测试等）时只记日志，不影响数据写入。
 */
final class TaskReminders {
    private static final String TAG = "TaskReminders";

    private TaskReminders() {
    }

    /** 需要提醒且提醒时刻在未来时安排（REPLACE），否则取消已有提醒 */
    static void sync(Context app, FarmTaskEntity task, long now) {
        try {
            if (TaskSchedule.shouldRemind(task, now)) {
                FarmTaskReminderWorker.schedule(app, task);
            } else {
                FarmTaskReminderWorker.cancel(app, task.id);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "WorkManager unavailable", e);
        }
    }

    static void cancel(Context app, long taskId) {
        try {
            FarmTaskReminderWorker.cancel(app, taskId);
        } catch (IllegalStateException e) {
            Log.w(TAG, "WorkManager unavailable", e);
        }
    }
}
