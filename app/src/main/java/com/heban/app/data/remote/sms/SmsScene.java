package com.heban.app.data.remote.sms;

import com.heban.app.BuildConfig;

/** 验证码使用场景与阿里云短信认证赠送模板（技术准备文档表 8-4）。 */
public enum SmsScene {
    /** A04 验证码登录 / 自动注册 */
    LOGIN,
    /** A05 注册 */
    REGISTER,
    /** A07 忘记密码、M07 修改密码 */
    RESET_PASSWORD,
    /** M08 换绑手机号、A09 申诉换绑：验证新手机号 */
    CHANGE_PHONE;

    public String templateCode() {
        switch (this) {
            case RESET_PASSWORD:
                return BuildConfig.ALIYUN_SMS_TPL_RESET;
            case CHANGE_PHONE:
                return BuildConfig.ALIYUN_SMS_TPL_CHANGE_PHONE;
            case LOGIN:
            case REGISTER:
            default:
                return BuildConfig.ALIYUN_SMS_TPL_LOGIN;
        }
    }
}
