package com.heban.app.data.remote.weather;

/**
 * 和风天气的错误返回体。新版为 HTTP 状态码 + application/problem+json：
 * {"error":{"status":401,"type":"https://dev.qweather.com/docs/resource/error-code/#unauthorized","title":"Unauthorized",...}}；
 * 旧版为 HTTP 200 + {"code":"401"}。两种都兼容。
 */
public class QwErrorResp {
    public String code;
    public Error error;

    public static class Error {
        public int status;
        public String type;
        public String title;
        public String detail;
    }
}
