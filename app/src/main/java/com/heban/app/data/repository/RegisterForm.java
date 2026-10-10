package com.heban.app.data.repository;

import java.util.ArrayList;
import java.util.List;

/** A05 注册 + A06 完善资料 收集的信息。手机号在 A05 已通过验证码校验。 */
public final class RegisterForm {
    public String phone;
    public String password;
    /** A06 跳过时为 null，由仓库生成默认昵称「农友 + 手机号后四位」 */
    public String nickname;
    public String region;
    /** 种植户 / 家庭园艺 / 农技人员 */
    public String identity;
    public List<String> crops = new ArrayList<>();
}
