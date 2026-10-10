package com.heban.app.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.heban.app.HeBanApp;
import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.di.AppContainer;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;

/**
 * 农事任务提醒（技术准备文档 11.1）：唯一任务 task-{id}，延迟到「农事时间 − 提前量」；
 * 保存时创建、修改时 REPLACE，删除或完成时取消。
 * 到点时任务已完成、已删除或不属于当前登录账号则不提醒。
 */
public class FarmTaskReminderWorker extends RxWorker {
    public static final String KEY_TASK_ID = "task_id";

    public FarmTaskReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Single<Result> createWork() {
        long taskId = getInputData().getLong(KEY_TASK_ID, -1);
        AppContainer c = HeBanApp.container(getApplicationContext());
        return c.db.farmTaskDao().getOnce(taskId)
                .flatMap(task -> task.done ? Single.just(Result.success())
                        : c.currentUser.userId().flatMap(uid -> uid != task.userId
                        ? Single.just(Result.success())
                        : c.notifier.showTaskReminder(task).andThen(Single.just(Result.success()))))
                .onErrorReturnItem(Result.failure());
    }

    /** 唯一任务名 */
    public static String uniqueName(long taskId) {
        return "task-" + taskId;
    }

    public static void schedule(Context ctx, FarmTaskEntity task) {
        long delay = task.dueAt - task.remindMinutes * 60_000L - System.currentTimeMillis();
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(FarmTaskReminderWorker.class)
                .setInitialDelay(Math.max(0, delay), TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder().putLong(KEY_TASK_ID, task.id).build())
                .build();
        WorkManager.getInstance(ctx).enqueueUniqueWork(uniqueName(task.id),
                ExistingWorkPolicy.REPLACE, req);
    }

    public static void cancel(Context ctx, long taskId) {
        WorkManager.getInstance(ctx).cancelUniqueWork(uniqueName(taskId));
    }
}
