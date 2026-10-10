package com.heban.app.data.model;

import java.util.ArrayList;
import java.util.List;

/** 作物生长期表（assets/mock/crop_profiles.json）：默认生长期、生长阶段与图标。 */
public final class CropProfile {
    /** 作物名称，如「番茄」 */
    public String name;
    /** 默认生长期（定植到采收，天） */
    public int expectedDays;
    /** Material Symbol 名称，对应 @drawable/ic_<icon>，如 nutrition / eco / grass / psychiatry */
    public String icon;
    /** 示例图片相对路径，如 images/crops/tomato.jpg（没有图片时为空，界面显示 icon 占位） */
    public String image;
    /** 生长阶段，第一个通常是「育苗」（定植前，视为已完成） */
    public List<Stage> stages = new ArrayList<>();

    public static final class Stage {
        public String name;
        /** 从定植日起第几天进入该阶段；育苗为负数 */
        public int startDay;
    }
}
