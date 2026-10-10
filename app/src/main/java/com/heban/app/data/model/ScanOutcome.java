package com.heban.app.data.model;

import androidx.annotation.Nullable;

/** 一次识别的结果（D03）：解析后的结果、识别服务名、保存的记录与照片、匹配到的本地百科条目。 */
public final class ScanOutcome {
    public ScanResult result;
    /** 识别服务显示名，用于「由 DeepSeek 识别」 */
    public String provider;
    public long recordId;
    public String imagePath;
    public ScanMode mode = ScanMode.PEST;
    /** top.name 按 pest 表名称 / 别名匹配到的百科 id，未匹配为 null（「查看防治方法」跳 D05） */
    @Nullable public Long pestId;
    /** 已保存到的作物 id，未保存为 null */
    @Nullable public Long cropId;
    public long createdAt;
}
