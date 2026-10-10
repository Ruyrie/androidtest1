package com.heban.app.data.model;

import androidx.annotation.DrawableRes;

/** 农事指数（H02 六宫格），由 FarmIndexCalculator 根据预报计算（技术准备文档表 8-3）。 */
public final class FarmIndex {
    /** 喷药 / 灌溉 / 施肥 / 晾晒 / 霜冻 / 通风 */
    public String name;
    /** 适宜 / 较适宜 / 不宜 / 暂不需要 / 12日起注意 / 上午为宜 …… */
    public String level;
    /** 补充说明，如推荐时段「15:00–18:00」，可为空 */
    public String detail;
    @DrawableRes public int icon;
    /** 是否为提醒类（霜冻等，用 Warning / Error 色） */
    public boolean warning;
}
