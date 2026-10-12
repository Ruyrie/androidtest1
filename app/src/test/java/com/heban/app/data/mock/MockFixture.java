package com.heban.app.data.mock;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.local.entity.ProductEntity;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** 测试用：直接读取 app/src/main/assets/mock/ 下的 pests.json、products.json */
public final class MockFixture {
    private MockFixture() {
    }

    public static File assetFile(String asset) {
        File f = new File("src/main/assets/" + asset);
        if (!f.exists()) f = new File("app/src/main/assets/" + asset);
        return f;
    }

    public static File resFile(String relative) {
        File f = new File("src/main/res/" + relative);
        if (!f.exists()) f = new File("app/src/main/res/" + relative);
        return f;
    }

    public static List<PestEntity> pests() throws Exception {
        try (Reader r = reader(MockJson.PESTS)) {
            return MockJson.parsePests(new Gson(), r);
        }
    }

    public static List<ProductEntity> products(long now) throws Exception {
        try (Reader r = reader(MockJson.PRODUCTS)) {
            return MockJson.parseProducts(new Gson(), r, now);
        }
    }

    public static PestEntity pest(String name) throws Exception {
        for (PestEntity p : pests()) {
            if (p.name.equals(name)) return p;
        }
        throw new AssertionError("no pest " + name);
    }

    private static Reader reader(String asset) throws Exception {
        return new InputStreamReader(Files.newInputStream(assetFile(asset).toPath()), StandardCharsets.UTF_8);
    }
}
