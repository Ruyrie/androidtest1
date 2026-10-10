package com.heban.app.data.remote.api;

import com.heban.app.data.local.entity.AppealEntity;
import com.heban.app.data.repository.RegisterForm;

import java.util.ArrayList;
import java.util.List;

/** 账号接口（/auth/*）的请求与返回体。字段名即 JSON 键名，后端开发时以此为基础细化。 */
public final class AuthDtos {
    private AuthDtos() {
    }

    /** POST /auth/sms-code；scene 为 SmsScene 名称：LOGIN / REGISTER / RESET_PASSWORD / CHANGE_PHONE */
    public static class SmsCodeReq {
        public String phone;
        public String scene;

        public SmsCodeReq(String phone, String scene) {
            this.phone = phone;
            this.scene = scene;
        }
    }

    /** POST /auth/sms-code/verify（预留） */
    public static class VerifyCodeReq {
        public String phone;
        public String code;

        public VerifyCodeReq(String phone, String code) {
            this.phone = phone;
            this.code = code;
        }
    }

    /** POST /auth/login */
    public static class LoginReq {
        public String phone;
        public String password;

        public LoginReq(String phone, String password) {
            this.phone = phone;
            this.password = password;
        }
    }

    /** POST /auth/login-sms：未注册的手机号由后端自动创建账号 */
    public static class LoginSmsReq {
        public String phone;
        public String code;

        public LoginSmsReq(String phone, String code) {
            this.phone = phone;
            this.code = code;
        }
    }

    /** POST /auth/register（A05 + A06） */
    public static class RegisterReq {
        public String phone;
        public String password;
        /** A05 中已校验过的验证码，供后端复核 */
        public String code;
        public String nickname;
        public String region;
        public String identity;
        public List<String> crops = new ArrayList<>();

        public static RegisterReq from(RegisterForm form, String code) {
            RegisterReq r = new RegisterReq();
            r.phone = form.phone;
            r.password = form.password;
            r.code = code;
            r.nickname = form.nickname;
            r.region = form.region;
            r.identity = form.identity;
            if (form.crops != null) r.crops = new ArrayList<>(form.crops);
            return r;
        }
    }

    /** POST /auth/password/reset（A07 + A08） */
    public static class ResetPasswordReq {
        public String phone;
        public String code;
        public String newPassword;

        public ResetPasswordReq(String phone, String code, String newPassword) {
            this.phone = phone;
            this.code = code;
            this.newPassword = newPassword;
        }
    }

    /** POST /auth/appeal（A09） */
    public static class AppealReq {
        public String oldPhone;
        public String nickname;
        public String lastLogin;
        public String region;
        public String newPhone;
        /** 新手机号的验证码 */
        public String code;

        public static AppealReq from(AppealEntity a, String code) {
            AppealReq r = new AppealReq();
            r.oldPhone = a.oldPhone;
            r.nickname = a.nickname;
            r.lastLogin = a.lastLogin;
            r.region = a.region;
            r.newPhone = a.newPhone;
            r.code = code;
            return r;
        }
    }

    /** 登录 / 验证码登录 / 注册的返回 data */
    public static class AuthResp {
        public String token;
        public UserDto user;
    }

    /** 申诉的返回 data */
    public static class AppealResp {
        /** 如 HB20261010-0832 */
        public String appealNo;
        public String status;
    }
}
