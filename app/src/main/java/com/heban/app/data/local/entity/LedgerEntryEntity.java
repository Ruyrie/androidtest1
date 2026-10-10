package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 收支记账（D10 / D11；C04 可同步生成）。金额统一存「分」。 */
@Entity(tableName = "ledger_entry", indices = {@Index("user_id"), @Index("date")})
public class LedgerEntryEntity {
    public static final String TYPE_IN = "IN";
    public static final String TYPE_OUT = "OUT";
    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_RECORD = "record";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    /** IN / OUT */
    @NonNull
    public String type = TYPE_OUT;

    /** 类别：化肥、农药、种子种苗、人工、水电灌溉、农机、地租、其他；农产品销售、政府补贴、代耕作业、其他 */
    @NonNull
    public String category = "其他";

    @ColumnInfo(name = "amount_cent")
    public long amountCent;

    @ColumnInfo(name = "crop_id")
    public Long cropId;

    public long date;

    public String note;

    /** manual / record */
    @NonNull
    public String source = SOURCE_MANUAL;

    @ColumnInfo(name = "record_id")
    public Long recordId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
