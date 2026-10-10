package com.heban.app.data.repository;

import com.google.gson.Gson;
import com.heban.app.data.model.CropProfile;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** 测试用：直接读取 app/src/main/assets/mock/crop_profiles.json */
final class ProfileFixture {
    private static List<CropProfile> cache;

    private ProfileFixture() {
    }

    static File assetFile() {
        File f = new File("src/main/assets/" + CropProfiles.ASSET);
        if (!f.exists()) f = new File("app/src/main/assets/" + CropProfiles.ASSET);
        return f;
    }

    static File resFile(String relative) {
        File f = new File("src/main/res/" + relative);
        if (!f.exists()) f = new File("app/src/main/res/" + relative);
        return f;
    }

    static synchronized List<CropProfile> profiles() throws Exception {
        if (cache == null) {
            try (Reader r = new InputStreamReader(Files.newInputStream(assetFile().toPath()), StandardCharsets.UTF_8)) {
                cache = CropProfiles.parse(new Gson(), r);
            }
        }
        return cache;
    }

    static CropProfile profile(String name) throws Exception {
        return CropProfiles.match(profiles(), name);
    }
}
