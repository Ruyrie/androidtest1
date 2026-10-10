package com.heban.app.data.remote.sms;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.GET;
import retrofit2.http.Url;

/** 号码认证服务 RPC 接口：地址由 {@link AliyunSigner#signedUrl} 生成（含全部参数与签名）。 */
public interface AliyunApi {
    @GET
    Single<AliyunResp> call(@Url String signedUrl);
}
