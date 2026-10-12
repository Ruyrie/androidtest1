package com.heban.app.data.mock;

import androidx.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.local.entity.ProductEntity;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * assets/mock/ 中 JSON 文件的解析（纯 Java，可在 JVM 单元测试中直接调用）。
 * <ul>
 *   <li>键名与 Room 列名一致（下划线命名，如 crop_tags、price_cent）；</li>
 *   <li>实体中保存 JSON 字符串的字段（images、tags、prevention_agri …）在文件里直接写数组，解析时转成 JSON 字符串；</li>
 *   <li>products.json 用 created_hours_ago（几小时前发布）代替绝对时间，导入时按当前时间换算成 created_at，
 *       并强制 user_id = 0、mine = false（模拟商品不属于任何账号）。</li>
 * </ul>
 */
public final class MockJson {
    public static final String PESTS = "mock/pests.json";
    public static final String PRODUCTS = "mock/products.json";
    /** products.json 中的相对发布时间（小时） */
    public static final String CREATED_HOURS_AGO = "created_hours_ago";
    private static final long HOUR_MS = 60L * 60 * 1000;

    private MockJson() {
    }

    /** 在给定 Gson 的基础上：下划线键名 + 数组 / 对象可读入 String 字段 */
    public static Gson mockGson(@NonNull Gson base) {
        return base.newBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(String.class, new LenientStringAdapter())
                .create();
    }

    public static List<PestEntity> parsePests(@NonNull Gson base, @NonNull Reader reader) {
        PestEntity[] arr = mockGson(base).fromJson(reader, PestEntity[].class);
        if (arr == null) return Collections.emptyList();
        return new ArrayList<>(Arrays.asList(arr));
    }

    /** now：导入时刻（毫秒），created_at = now − created_hours_ago */
    public static List<ProductEntity> parseProducts(@NonNull Gson base, @NonNull Reader reader, long now) {
        JsonElement root = JsonParser.parseReader(reader);
        if (root == null || !root.isJsonArray()) return Collections.emptyList();
        Gson gson = mockGson(base);
        JsonArray arr = root.getAsJsonArray();
        List<ProductEntity> out = new ArrayList<>(arr.size());
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject().deepCopy();
            JsonElement hours = o.remove(CREATED_HOURS_AGO);
            ProductEntity p = gson.fromJson(o, ProductEntity.class);
            double h = hours == null || hours.isJsonNull() ? 0 : hours.getAsDouble();
            p.createdAt = now - Math.round(h * HOUR_MS);
            p.userId = 0;
            p.mine = false;
            p.cropId = null;
            out.add(p);
        }
        return out;
    }

    /** String 字段：数组和对象原样保存为紧凑的 JSON 文本，数字 / 布尔转成字符串 */
    static final class LenientStringAdapter extends TypeAdapter<String> {
        @Override
        public void write(JsonWriter out, String value) throws IOException {
            out.value(value);
        }

        @Override
        public String read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            switch (token) {
                case NULL:
                    in.nextNull();
                    return null;
                case BEGIN_ARRAY:
                case BEGIN_OBJECT:
                    return JsonParser.parseReader(in).toString();
                case BOOLEAN:
                    return Boolean.toString(in.nextBoolean());
                default:
                    return in.nextString();
            }
        }
    }
}
