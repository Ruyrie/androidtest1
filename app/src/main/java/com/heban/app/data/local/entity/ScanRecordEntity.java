package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 拍照识别记录（D02「历史」、D03）。 */
@Entity(tableName = "scan_record", indices = @Index("user_id"))
public class ScanRecordEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    /** 压缩后保存在 App 私有目录的照片 */
    @ColumnInfo(name = "image_path")
    public String imagePath;

    /** ScanMode 名称：PEST / CROP */
    @NonNull
    public String mode = "PEST";

    /** 模型返回并解析通过的 JSON（ScanResult） */
    @ColumnInfo(name = "result_json")
    public String resultJson;

    /** 识别服务显示名，如「DeepSeek」「通义千问 VL」 */
    public String provider;

    /** 保存到的作物，未保存为 null */
    @ColumnInfo(name = "crop_id")
    public Long cropId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
