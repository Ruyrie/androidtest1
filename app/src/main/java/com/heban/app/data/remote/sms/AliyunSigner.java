package com.heban.app.data.remote.sms;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 阿里云 RPC 风格请求签名（HMAC-SHA1，技术准备文档 8.3）。
 * 以后官方要求 V3 签名时，只需替换这一个类。
 */
public final class AliyunSigner {
    public static final String ENDPOINT = "https://dypnsapi.aliyuncs.com/";
    public static final String VERSION = "2017-05-25";

    private AliyunSigner() {
    }

    /** 返回已签名的完整 GET 地址（随机 SignatureNonce、当前 UTC 时间） */
    public static String signedUrl(String action, Map<String, String> bizParams,
                                   String akId, String akSecret) {
        return signedUrl(action, bizParams, akId, akSecret,
                UUID.randomUUID().toString(), timestamp(new Date()));
    }

    /** 指定 SignatureNonce 与 Timestamp（yyyy-MM-dd'T'HH:mm:ss'Z'，UTC），结果可复现，便于测试 */
    public static String signedUrl(String action, Map<String, String> bizParams,
                                   String akId, String akSecret,
                                   String nonce, String timestamp) {
        TreeMap<String, String> p = new TreeMap<>(bizParams);   // 参数按名称排序
        p.put("Action", action);
        p.put("Version", VERSION);
        p.put("Format", "JSON");
        p.put("AccessKeyId", akId);
        p.put("SignatureMethod", "HMAC-SHA1");
        p.put("SignatureVersion", "1.0");
        p.put("SignatureNonce", nonce);
        p.put("Timestamp", timestamp);

        String query = canonicalQuery(p);
        return ENDPOINT + "?" + query + "&Signature=" + enc(sign(query, akSecret));
    }

    /** 排序后的参数拼成 a=1&b=2，名称和值都按 {@link #enc} 编码 */
    static String canonicalQuery(SortedMap<String, String> sortedParams) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> e : sortedParams.entrySet()) {
            if (query.length() > 0) query.append('&');
            query.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        return query.toString();
    }

    /** StringToSign = "GET&%2F&" + enc(query)，密钥为 AccessKeySecret + "&"，结果 Base64 */
    static String sign(String canonicalQuery, String akSecret) {
        String stringToSign = "GET&" + enc("/") + "&" + enc(canonicalQuery);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            byte[] key = (akSecret + "&").getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 不可用", e);
        }
    }

    /** 阿里云要求的编码：UTF-8 百分号编码，空格 %20、* 为 %2A、~ 不编码 */
    public static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8")
                    .replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);   // UTF-8 一定可用
        }
    }

    /** ISO 8601 UTC 时间，如 2026-10-10T08:30:00Z */
    public static String timestamp(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(date);
    }
}
