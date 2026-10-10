package com.heban.app.data.remote.sms;

import com.google.gson.annotations.SerializedName;

/** SendSmsVerifyCode / CheckSmsVerifyCode 的返回体（Format=JSON）。 */
public class AliyunResp {
    public static final String CODE_OK = "OK";
    public static final String VERIFY_PASS = "PASS";

    /** OK 表示成功，其余为错误码（如 isv.BUSINESS_LIMIT_CONTROL） */
    @SerializedName("Code")
    public String code;

    @SerializedName("Message")
    public String message;

    @SerializedName("RequestId")
    public String requestId;

    @SerializedName("Success")
    public Boolean success;

    @SerializedName("Model")
    public Model model;

    public boolean isOk() {
        return CODE_OK.equals(code);
    }

    public static class Model {
        /** CheckSmsVerifyCode：PASS 表示校验通过 */
        @SerializedName("VerifyResult")
        public String verifyResult;

        /** SendSmsVerifyCode：回执 ID */
        @SerializedName("BizId")
        public String bizId;

        @SerializedName("OutId")
        public String outId;
    }
}
