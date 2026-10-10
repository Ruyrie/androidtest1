package com.heban.app.data.local.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.heban.app.data.local.dao.AppealDao;
import com.heban.app.data.local.dao.ChatDao;
import com.heban.app.data.local.dao.CropDao;
import com.heban.app.data.local.dao.FarmRecordDao;
import com.heban.app.data.local.dao.FarmTaskDao;
import com.heban.app.data.local.dao.FavoriteDao;
import com.heban.app.data.local.dao.LedgerDao;
import com.heban.app.data.local.dao.PestDao;
import com.heban.app.data.local.dao.PlotDao;
import com.heban.app.data.local.dao.PointsDao;
import com.heban.app.data.local.dao.ProductDao;
import com.heban.app.data.local.dao.ScanRecordDao;
import com.heban.app.data.local.dao.UserDao;
import com.heban.app.data.local.dao.WeatherDao;
import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.ChatSessionEntity;
import com.heban.app.data.local.entity.CheckinEntity;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.local.entity.PointsTxnEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.local.entity.ScanRecordEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.local.entity.WeatherCacheEntity;

/**
 * 本地数据库 heban.db（技术准备文档 7.1）。各阶段在 entities 中追加表。
 * 开发阶段允许破坏性迁移；正式发布前固定版本并编写 Migration。
 */
@Database(entities = {
        UserEntity.class, AppealEntity.class,
        PlotEntity.class, CropEntity.class, FarmRecordEntity.class, FarmTaskEntity.class,
        LedgerEntryEntity.class, WeatherCacheEntity.class,
        ChatSessionEntity.class, ChatMessageEntity.class, ScanRecordEntity.class, PestEntity.class,
        ProductEntity.class, FavoriteEntity.class, CheckinEntity.class, PointsTxnEntity.class
}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract AppealDao appealDao();

    public abstract PlotDao plotDao();

    public abstract CropDao cropDao();

    public abstract FarmRecordDao farmRecordDao();

    public abstract FarmTaskDao farmTaskDao();

    public abstract LedgerDao ledgerDao();

    public abstract WeatherDao weatherDao();

    public abstract ChatDao chatDao();

    public abstract ScanRecordDao scanRecordDao();

    public abstract PestDao pestDao();

    public abstract ProductDao productDao();

    public abstract FavoriteDao favoriteDao();

    public abstract PointsDao pointsDao();

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
