package com.heban.app.data.model;

import java.util.Objects;

/** 动态、评论、私信中显示的作者（农友或当前账号）。头像底色由 UI 按 farmerId / userId 固定取色。 */
public final class Author {
    /** 农友 id；作者为当前账号时为 0 */
    public long farmerId;
    /** 作者为当前账号时为其 id，否则为 0 */
    public long userId;
    public String name;
    public String avatarText;
    public String avatarPath;
    /** 徽标文字：种植能手 / 农技专家 / 官方 / 机构 / Lv.3 */
    public String badge;
    public boolean verified;
    public String city;
    /** 作者是当前账号（显示「老李种菜（我）」等） */
    public boolean me;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Author)) return false;
        Author a = (Author) o;
        return farmerId == a.farmerId && userId == a.userId && verified == a.verified && me == a.me
                && Objects.equals(name, a.name) && Objects.equals(avatarText, a.avatarText)
                && Objects.equals(avatarPath, a.avatarPath) && Objects.equals(badge, a.badge)
                && Objects.equals(city, a.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(farmerId, userId, name);
    }
}
