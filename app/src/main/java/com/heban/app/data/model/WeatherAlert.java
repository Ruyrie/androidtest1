package com.heban.app.data.model;

/** App 本地计算的天气预警（降温预警等），显示在 H01 / H02 顶部 Warning 提示栏。 */
public final class WeatherAlert {
    /** 如「降温预警」 */
    public String title;
    /** H01 短文案，如「12日起最低 8°C，注意大棚保温」 */
    public String shortText;
    /** H02 详细文案，如「12–14 日受冷空气影响，最低 7°C，大棚夜间注意加盖保温被」 */
    public String detailText;
}
