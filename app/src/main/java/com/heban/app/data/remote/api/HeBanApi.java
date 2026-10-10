package com.heban.app.data.remote.api;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 禾伴后端接口（技术准备文档表 8-6，预留）。地址为 BuildConfig.API_BASE_URL，统一返回 {@link BaseResponse}。
 * 后端尚未开发，路径与字段在后端开发时细化。
 */
public interface HeBanApi {

    // —— 账号 ——

    @POST("auth/sms-code")
    Single<BaseResponse<Object>> sendSmsCode(@Body AuthDtos.SmsCodeReq body);

    /** 表 8-6 未列出：校验验证码（A05 第 1 步、A07、A09 第 2 步），后端开发时确认 */
    @POST("auth/sms-code/verify")
    Single<BaseResponse<Object>> verifySmsCode(@Body AuthDtos.VerifyCodeReq body);

    /** 表 8-6 未列出：手机号是否已注册，data 为 true / false，后端开发时确认 */
    @GET("auth/registered")
    Single<BaseResponse<Boolean>> isRegistered(@Query("phone") String phone);

    @POST("auth/login")
    Single<BaseResponse<AuthDtos.AuthResp>> login(@Body AuthDtos.LoginReq body);

    @POST("auth/login-sms")
    Single<BaseResponse<AuthDtos.AuthResp>> loginSms(@Body AuthDtos.LoginSmsReq body);

    @POST("auth/register")
    Single<BaseResponse<AuthDtos.AuthResp>> register(@Body AuthDtos.RegisterReq body);

    @POST("auth/password/reset")
    Single<BaseResponse<Object>> resetPassword(@Body AuthDtos.ResetPasswordReq body);

    @POST("auth/appeal")
    Single<BaseResponse<AuthDtos.AppealResp>> appeal(@Body AuthDtos.AppealReq body);

    // —— 用户 ——

    /** authorization 为 "Bearer &lt;token&gt;"；为 null 时不带该请求头 */
    @GET("users/me")
    Single<BaseResponse<UserDto>> me(@Header("Authorization") String authorization);
}
