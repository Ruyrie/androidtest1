package com.heban.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** 识图模型返回的 JSON（技术准备文档 9.5 / 表 9-3），用 Gson 解析。 */
public final class ScanResult {
    @SerializedName("image_ok") public boolean imageOk = true;
    @SerializedName("image_issue") public String imageIssue;
    /** 识别出的作物，如「黄瓜」 */
    public String crop;
    public Top top;
    public List<Other> others = new ArrayList<>();

    public static final class Top {
        public String name;
        /** 真菌 / 细菌 / 卵菌 / 病毒病害、虫害、生理性、杂草、健康 */
        public String category;
        public String part;
        /** 0–1 */
        public double confidence;
        /** 轻度 / 中度 / 重度 */
        public String severity;
        public String evidence;
        public List<String> advice = new ArrayList<>();
        @SerializedName("urgency_days") public int urgencyDays;
    }

    public static final class Other {
        public String name;
        public double confidence;
    }
}
