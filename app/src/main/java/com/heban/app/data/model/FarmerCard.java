package com.heban.app.data.model;

import com.heban.app.data.local.entity.FarmerEntity;

import java.util.Objects;

/** 农友（S06 主页、S07 列表）：资料 + 与当前账号的关注关系。 */
public final class FarmerCard {
    public FarmerEntity farmer;
    public FollowRelation relation = FollowRelation.NONE;
    /** 动态 / 问答 / 集市数量（S06 Tab 标题） */
    public int postCount;
    public int qaCount;
    public int marketCount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FarmerCard)) return false;
        FarmerCard c = (FarmerCard) o;
        return relation == c.relation && postCount == c.postCount && qaCount == c.qaCount
                && marketCount == c.marketCount && farmer != null && c.farmer != null
                && farmer.id == c.farmer.id && farmer.fansCount == c.farmer.fansCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(farmer == null ? 0 : farmer.id, relation);
    }
}
