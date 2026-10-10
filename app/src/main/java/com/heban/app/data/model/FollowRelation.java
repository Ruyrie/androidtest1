package com.heban.app.data.model;

/** 关注关系四种状态（S06 / S07），由双方关注标记计算。 */
public enum FollowRelation {
    /** 互不关注 → 按钮「关注」 */
    NONE("关注"),
    /** 我关注了对方 → 「已关注」 */
    FOLLOWING("已关注"),
    /** 对方关注了我 → 「回关」 */
    FOLLOWED_BY("回关"),
    /** 互相关注 */
    MUTUAL("互相关注");

    public final String label;

    FollowRelation(String label) {
        this.label = label;
    }

    public static FollowRelation of(boolean iFollow, boolean followsMe) {
        if (iFollow && followsMe) return MUTUAL;
        if (iFollow) return FOLLOWING;
        if (followsMe) return FOLLOWED_BY;
        return NONE;
    }
}
