package com.heban.app.data.model;

/** M11「模型服务」中用户填写的配置（技术准备文档表 9-5）。 */
public final class ModelServiceConfig {
    public static final String PROVIDER_DEEPSEEK = "deepseek";
    public static final String PROVIDER_DASHSCOPE = "dashscope";
    public static final String PROVIDER_CUSTOM = "custom";

    /** 是否启用用户配置；false 表示使用内置配置 */
    public boolean enabled;
    public String provider = PROVIDER_DEEPSEEK;
    /** 不带结尾斜杠 */
    public String baseUrl;
    /** 明文 Key，仅在保存 / 测试时使用；读取时为空（只给出 apiKeyLast4） */
    public String apiKey;
    /** 已保存 Key 的后 4 位，用于「已保存 ****abcd」 */
    public String apiKeyLast4;
    public String chatModel;
    /** 可为空：留空时识图使用内置 DeepSeek 视觉模型 */
    public String visionModel;
}
