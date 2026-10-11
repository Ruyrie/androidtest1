package com.heban.app.data.local.dao;

import androidx.room.Ignore;

/** D10 结余趋势：第 bucket 个月（0 为最早的月份）某种收支类型（IN / OUT）的合计（分）。 */
public class LedgerBucketSum {
    public int bucket;

    public String type;

    public long total;

    public LedgerBucketSum() {
    }

    /** 单元测试用 */
    @Ignore
    public LedgerBucketSum(int bucket, String type, long total) {
        this.bucket = bucket;
        this.type = type;
        this.total = total;
    }
}
