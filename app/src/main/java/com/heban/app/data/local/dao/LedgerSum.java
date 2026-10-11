package com.heban.app.data.local.dao;

import androidx.room.Ignore;

/** 记账按某一列分组的合计（D10）：name 为收支类型（IN / OUT）或类别，total 单位为分。 */
public class LedgerSum {
    public String name;

    public long total;

    public LedgerSum() {
    }

    /** 单元测试用 */
    @Ignore
    public LedgerSum(String name, long total) {
        this.name = name;
        this.total = total;
    }
}
