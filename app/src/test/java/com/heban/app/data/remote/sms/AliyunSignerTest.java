package com.heban.app.data.remote.sms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AliyunSignerTest {
    private static final String NONCE = "3ee8c1b8-83d3-44af-a94f-4e0ad82fd6cf";
    private static final String TIMESTAMP = "2026-10-10T08:30:00Z";

    private static Map<String, String> bizParams() {
        Map<String, String> m = new LinkedHashMap<>();   // 故意不排序
        m.put("TemplateParam", "{\"code\":\"##code##\",\"min\":\"5\"}");
        m.put("PhoneNumber", "13800005678");
        m.put("TemplateCode", "100001");
        m.put("SignName", "禾伴");
        return m;
    }

    @Test
    public void enc_followsAliyunRules() {
        assertEquals("a%20b", AliyunSigner.enc("a b"));
        assertEquals("%2A", AliyunSigner.enc("*"));
        assertEquals("~", AliyunSigner.enc("~"));
        assertEquals("-_.", AliyunSigner.enc("-_."));
        assertEquals("AZaz09", AliyunSigner.enc("AZaz09"));
        assertEquals("a%2Bb%3Dc%26d%2Fe%3A", AliyunSigner.enc("a+b=c&d/e:"));
        assertEquals("%E7%A6%BE%E4%BC%B4", AliyunSigner.enc("禾伴"));
        assertEquals("%7B%22code%22%3A%22%23%23code%23%23%22%2C%22min%22%3A%225%22%7D",
                AliyunSigner.enc("{\"code\":\"##code##\",\"min\":\"5\"}"));
    }

    @Test
    public void timestamp_isUtcIso8601() {
        assertEquals("1970-01-01T00:00:00Z", AliyunSigner.timestamp(new Date(0)));
        assertEquals("2026-10-10T08:30:00Z", AliyunSigner.timestamp(new Date(1791621000000L)));
    }

    /** 阿里云官方文档「RPC 签名机制」中的示例（ECS DescribeRegions，AccessKeySecret = testsecret） */
    @Test
    public void sign_matchesOfficialDocumentExample() {
        TreeMap<String, String> p = new TreeMap<>();
        p.put("Timestamp", "2016-02-23T12:46:24Z");
        p.put("Format", "XML");
        p.put("AccessKeyId", "testid");
        p.put("Action", "DescribeRegions");
        p.put("SignatureMethod", "HMAC-SHA1");
        p.put("SignatureNonce", NONCE);
        p.put("Version", "2014-05-26");
        p.put("SignatureVersion", "1.0");

        String query = AliyunSigner.canonicalQuery(p);
        assertEquals("AccessKeyId=testid&Action=DescribeRegions&Format=XML&SignatureMethod=HMAC-SHA1"
                + "&SignatureNonce=" + NONCE + "&SignatureVersion=1.0"
                + "&Timestamp=2016-02-23T12%3A46%3A24Z&Version=2014-05-26", query);
        assertEquals("OLeaidS1JvxuMvnyHOwuJ+uX5qY=", AliyunSigner.sign(query, "testsecret"));
    }

    @Test
    public void signedUrl_isDeterministicWithFixedNonceAndTimestamp() {
        String a = AliyunSigner.signedUrl("SendSmsVerifyCode", bizParams(), "testid", "testsecret", NONCE, TIMESTAMP);
        String b = AliyunSigner.signedUrl("SendSmsVerifyCode", bizParams(), "testid", "testsecret", NONCE, TIMESTAMP);
        assertEquals(a, b);

        String otherNonce = AliyunSigner.signedUrl("SendSmsVerifyCode", bizParams(), "testid", "testsecret",
                "another-nonce", TIMESTAMP);
        assertNotEquals(signatureOf(a), signatureOf(otherNonce));
        String otherSecret = AliyunSigner.signedUrl("SendSmsVerifyCode", bizParams(), "testid", "other",
                NONCE, TIMESTAMP);
        assertNotEquals(signatureOf(a), signatureOf(otherSecret));
    }

    /** 不经过 AliyunSigner：手写排序后的查询串，并用独立的编码与 HMAC-SHA1 重新计算签名 */
    @Test
    public void signedUrl_matchesIndependentComputation() throws Exception {
        String url = AliyunSigner.signedUrl("SendSmsVerifyCode", bizParams(), "testid", "testsecret", NONCE, TIMESTAMP);

        String expectedQuery = "AccessKeyId=testid"
                + "&Action=SendSmsVerifyCode"
                + "&Format=JSON"
                + "&PhoneNumber=13800005678"
                + "&SignName=%E7%A6%BE%E4%BC%B4"
                + "&SignatureMethod=HMAC-SHA1"
                + "&SignatureNonce=" + NONCE
                + "&SignatureVersion=1.0"
                + "&TemplateCode=100001"
                + "&TemplateParam=%7B%22code%22%3A%22%23%23code%23%23%22%2C%22min%22%3A%225%22%7D"
                + "&Timestamp=2026-10-10T08%3A30%3A00Z"
                + "&Version=2017-05-25";
        String stringToSign = "GET&%2F&" + percentEncode(expectedQuery);
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec("testsecret&".getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        String signature = Base64.getEncoder().encodeToString(
                mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));

        // 与 Python（urllib.parse.quote + hmac）计算的结果一致
        assertEquals("kx0nueAeSQgXeqkgK+IJJBfvka0=", signature);
        assertEquals("https://dypnsapi.aliyuncs.com/?" + expectedQuery
                + "&Signature=" + percentEncode(signature), url);
    }

    @Test
    public void signedUrl_containsCommonParameters() {
        String url = AliyunSigner.signedUrl("CheckSmsVerifyCode", new TreeMap<>(), "id", "secret", NONCE, TIMESTAMP);
        assertTrue(url.startsWith(AliyunSigner.ENDPOINT + "?AccessKeyId=id&Action=CheckSmsVerifyCode&Format=JSON&"));
        assertTrue(url.contains("&SignatureMethod=HMAC-SHA1&"));
        assertTrue(url.contains("&SignatureVersion=1.0&"));
        assertTrue(url.contains("&Version=2017-05-25&Signature="));
    }

    private static String signatureOf(String url) {
        return url.substring(url.indexOf("&Signature=") + "&Signature=".length());
    }

    /** RFC 3986：只保留 A–Z a–z 0–9 - _ . ~，其余按 UTF-8 字节编码为 %XX（大写） */
    private static String percentEncode(String s) {
        StringBuilder sb = new StringBuilder();
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            boolean unreserved = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~';
            if (unreserved) {
                sb.append((char) c);
            } else {
                sb.append('%').append(String.format("%02X", c));
            }
        }
        return sb.toString();
    }
}
