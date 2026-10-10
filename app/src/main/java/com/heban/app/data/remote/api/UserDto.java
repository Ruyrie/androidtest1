package com.heban.app.data.remote.api;

import com.heban.app.data.local.entity.UserEntity;

import java.util.List;

/** 后端返回的用户资料（GET /users/me、登录 / 注册返回体中的 user）。 */
public class UserDto {
    public long id;
    public String phone;
    public String nickname;
    public String region;
    /** 种植户 / 家庭园艺 / 农技人员 */
    public String identity;
    public List<String> crops;
    public String bio;
    public int years;
    /** 毫秒时间戳 */
    public long createdAt;

    /** 转成本地 user 表缓存（密码相关列为空） */
    public UserEntity toEntity() {
        UserEntity u = new UserEntity();
        u.id = id;
        u.phone = phone == null ? "" : phone;
        u.nickname = nickname == null ? "" : nickname;
        u.region = region;
        u.identity = identity;
        u.crops = crops == null || crops.isEmpty() ? null : String.join(",", crops);
        u.bio = bio;
        u.years = years;
        u.createdAt = createdAt;
        return u;
    }
}
