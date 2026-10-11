package com.heban.app.data.remote.weather;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.remote.AppError;

import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * 和风天气错误 → 中文提示（错误码见 dev.qweather.com/docs/resource/error-code/）。
 * 界面可用 {@link #isNotConfigured(AppError)} / {@link #isNoLocation(AppError)} 判断是否显示配置说明或「选择地点」入口。
 */
public final class QWeatherErrors {
    public static final String CODE_NOT_CONFIGURED = "WEATHER_NOT_CONFIGURED";
    public static final String CODE_NO_LOCATION = "WEATHER_NO_LOCATION";

    public static final String MSG_NOT_CONFIGURED =
            "未配置天气服务，请在 local.properties 中填写和风天气的 API Host 和 Key";
    public static final String MSG_NO_LOCATION = "请选择地点";
    public static final String MSG_UNAUTHORIZED = "天气服务认证失败，请检查和风天气的 API Key";
    public static final String MSG_NO_CREDIT = "天气服务额度不足，请到和风天气控制台查看";
    public static final String MSG_OVERDUE = "和风天气帐号有逾期账单，请到控制台处理";
    public static final String MSG_INVALID_HOST = "天气服务 API Host 不正确，请在和风天气控制台「设置」中查看";
    public static final String MSG_SECURITY = "天气请求被安全限制拒绝，请检查和风天气控制台中的请求限制";
    public static final String MSG_SUSPENDED = "和风天气帐号已被冻结，请到控制台查看";
    public static final String MSG_DEPRECATED = "天气接口已停用，请更新 App";
    public static final String MSG_FORBIDDEN = "没有权限获取天气数据，请检查和风天气控制台设置";
    public static final String MSG_NO_SUCH_LOCATION = "没有找到该地点的天气数据，请重新选择地点";
    public static final String MSG_BAD_PARAM = "天气请求参数有误，请重新选择地点";
    public static final String MSG_NOT_FOUND = "天气接口地址不正确，请检查和风天气的 API Host";
    public static final String MSG_TOO_MANY = "天气请求太频繁，请稍后再试";
    public static final String MSG_MONTHLY_LIMIT = "本月天气请求量已用完，请下月再试";
    public static final String MSG_SERVER = "天气服务暂时不可用，请稍后重试";
    public static final String MSG_UNKNOWN = "获取天气失败，请稍后重试";

    private static final Gson GSON = new Gson();

    private QWeatherErrors() {
    }

    public static QWeatherException notConfigured() {
        return new QWeatherException(CODE_NOT_CONFIGURED, MSG_NOT_CONFIGURED, AppError.Type.BUSINESS, 0);
    }

    public static QWeatherException noLocation() {
        return new QWeatherException(CODE_NO_LOCATION, MSG_NO_LOCATION, AppError.Type.BUSINESS, 0);
    }

    public static boolean isNotConfigured(@Nullable AppError e) {
        return e != null && MSG_NOT_CONFIGURED.equals(e.message);
    }

    /** 无法确定地点（没有保存的地点、没有定位、资料中没有地区）：界面提供「选择地点」 */
    public static boolean isNoLocation(@Nullable AppError e) {
        return e != null && MSG_NO_LOCATION.equals(e.message);
    }

    /** 返回体中的 code 不为 "200" 时抛出（旧版错误格式）；code 缺失视为成功 */
    public static void ensureOk(@Nullable String code) throws QWeatherException {
        if (code == null || code.isEmpty() || "200".equals(code)) return;
        throw fromCode(code);
    }

    /** 旧版 code（204 / 400 / 401 / 402 / 403 / 404 / 429 / 500） */
    @NonNull
    public static QWeatherException fromCode(@NonNull String code) {
        int c;
        try {
            c = Integer.parseInt(code.trim());
        } catch (NumberFormatException e) {
            c = 0;
        }
        switch (c) {
            case 204:
            case 404:
                return ex(code, MSG_NO_SUCH_LOCATION, AppError.Type.BUSINESS, c);
            case 400:
                return ex(code, MSG_BAD_PARAM, AppError.Type.BUSINESS, c);
            default:
                return fromStatus(c, code);
        }
    }

    /** Retrofit 的 HTTP 错误：解析 problem+json 中的 error.type / title，或旧版 code */
    @NonNull
    public static QWeatherException fromHttp(@NonNull HttpException e) {
        int status = e.code();
        QwErrorResp body = parseBody(e.response());
        if (body != null && body.error != null) {
            String kind = normalize(body.error.type) + " " + normalize(body.error.title);
            QWeatherException byType = fromType(status, kind);
            if (byType != null) return byType;
        }
        if (body != null && body.code != null && !"200".equals(body.code)) {
            return fromCode(body.code);
        }
        if (status == 404) return ex(String.valueOf(status), MSG_NOT_FOUND, AppError.Type.BUSINESS, status);
        if (status == 400) return ex(String.valueOf(status), MSG_BAD_PARAM, AppError.Type.BUSINESS, status);
        return fromStatus(status, String.valueOf(status));
    }

    /** 把 Retrofit 的 HttpException 换成带中文提示的 QWeatherException，其余异常原样返回 */
    @NonNull
    public static Throwable wrap(@NonNull Throwable t) {
        Throwable u = unwrap(t);
        if (u instanceof HttpException) return fromHttp((HttpException) u);
        return u;
    }

    /** 转换为界面状态中的 AppError（网络异常沿用 AppError.from 的离线提示） */
    @NonNull
    public static AppError toAppError(@NonNull Throwable t) {
        Throwable u = wrap(t);
        if (u instanceof QWeatherException) return ((QWeatherException) u).toAppError();
        return AppError.from(u);
    }

    /** blockingGet 会把受检异常包进 RuntimeException，这里取出原始异常 */
    @NonNull
    public static Throwable unwrap(@NonNull Throwable t) {
        Throwable cur = t;
        while (cur.getClass() == RuntimeException.class && cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur;
    }

    @Nullable
    private static QWeatherException fromType(int status, String kind) {
        String s = String.valueOf(status);
        if (kind.contains("nosuchlocation")) return ex(s, MSG_NO_SUCH_LOCATION, AppError.Type.BUSINESS, status);
        if (kind.contains("datanotavailable")) return ex(s, MSG_NO_SUCH_LOCATION, AppError.Type.BUSINESS, status);
        if (kind.contains("invalidparameter") || kind.contains("missingparameter")) {
            return ex(s, MSG_BAD_PARAM, AppError.Type.BUSINESS, status);
        }
        if (kind.contains("unauthorized")) return ex(s, MSG_UNAUTHORIZED, AppError.Type.UNAUTHORIZED, status);
        if (kind.contains("nocredit")) return ex(s, MSG_NO_CREDIT, AppError.Type.NO_BALANCE, status);
        if (kind.contains("overdue")) return ex(s, MSG_OVERDUE, AppError.Type.NO_BALANCE, status);
        if (kind.contains("securityrestriction")) return ex(s, MSG_SECURITY, AppError.Type.BUSINESS, status);
        if (kind.contains("invalidhost")) return ex(s, MSG_INVALID_HOST, AppError.Type.BUSINESS, status);
        if (kind.contains("accountsuspension")) return ex(s, MSG_SUSPENDED, AppError.Type.BUSINESS, status);
        if (kind.contains("deprecated")) return ex(s, MSG_DEPRECATED, AppError.Type.BUSINESS, status);
        if (kind.contains("overmonthlylimit")) return ex(s, MSG_MONTHLY_LIMIT, AppError.Type.RATE_LIMIT, status);
        if (kind.contains("toomanyrequests")) return ex(s, MSG_TOO_MANY, AppError.Type.RATE_LIMIT, status);
        if (kind.contains("forbidden")) return ex(s, MSG_FORBIDDEN, AppError.Type.BUSINESS, status);
        if (kind.contains("notfound")) return ex(s, MSG_NOT_FOUND, AppError.Type.BUSINESS, status);
        return null;
    }

    private static QWeatherException fromStatus(int status, String code) {
        if (status == 401) return ex(code, MSG_UNAUTHORIZED, AppError.Type.UNAUTHORIZED, status);
        if (status == 402) return ex(code, MSG_NO_CREDIT, AppError.Type.NO_BALANCE, status);
        if (status == 403) return ex(code, MSG_FORBIDDEN, AppError.Type.BUSINESS, status);
        if (status == 429) return ex(code, MSG_TOO_MANY, AppError.Type.RATE_LIMIT, status);
        if (status >= 500) return ex(code, MSG_SERVER, AppError.Type.SERVER, status);
        return ex(code, MSG_UNKNOWN, AppError.Type.UNKNOWN, status);
    }

    private static QWeatherException ex(String code, String msg, AppError.Type type, int status) {
        return new QWeatherException(code, msg, type, status);
    }

    /** "https://…/error-code/#no-such-location" / "No Such Location" → "nosuchlocation" */
    private static String normalize(@Nullable String s) {
        if (s == null) return "";
        String v = s;
        int hash = v.lastIndexOf('#');
        if (hash >= 0) v = v.substring(hash + 1);
        return v.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    @Nullable
    private static QwErrorResp parseBody(@Nullable Response<?> response) {
        if (response == null) return null;
        ResponseBody body = response.errorBody();
        if (body == null) return null;
        try {
            String s = body.string();
            if (s.trim().isEmpty()) return null;
            return GSON.fromJson(s, QwErrorResp.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
