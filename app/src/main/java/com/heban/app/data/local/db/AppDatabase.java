package com.heban.app.data.local.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.heban.app.data.local.dao.AppealDao;
import com.heban.app.data.local.dao.UserDao;
import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.UserEntity;

/**
 * 本地数据库 heban.db（技术准备文档 7.1）。各阶段在 entities 中追加表。
 * 开发阶段允许破坏性迁移；正式发布前固定版本并编写 Migration。
 */
@Database(entities = {
        UserEntity.class, AppealEntity.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract AppealDao appealDao();

    private static volatile AppDatabase instance;

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "heban.db")
                            .fallbackToDestructiveMigration(true)   // 仅开发阶段
                            .build();
                }
            }
        }
        return instance;
    }
}
