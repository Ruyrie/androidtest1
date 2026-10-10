package com.heban.app.data.model;

/** D10 支出构成中的一类（按金额倒序，前 3 类之外合并为「其他」）。 */
public final class CategoryShare {
    public final String name;
    public final long amountCent;
    /** 占比 0–100 */
    public final int percent;

    public CategoryShare(String name, long amountCent, int percent) {
        this.name = name;
        this.amountCent = amountCent;
        this.percent = percent;
    }
}
