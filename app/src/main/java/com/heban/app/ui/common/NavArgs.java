package com.heban.app.ui.common;

/** 页面之间传递参数的 Bundle 键（navigate(destId, bundle)），各模块共用。 */
public final class NavArgs {
    private NavArgs() {
    }

    /** long：作物 id（C02 详情、C03 编辑、C04 / H04 预选作物）；C03 中为 0 或缺省表示新增 */
    public static final String CROP_ID = "crop_id";
    /** long：地块 id（C03 预选地块、C05 编辑地块） */
    public static final String PLOT_ID = "plot_id";
    /** String：FarmType 名称（C04 记录类型、H04 农事类型预选） */
    public static final String FARM_TYPE = "farm_type";
    /** long：LocalDate.toEpochDay()（H03 打开某天、H04 预填日期） */
    public static final String EPOCH_DAY = "epoch_day";
    /** long：农事任务 id（H04 编辑） */
    public static final String TASK_ID = "task_id";
    /** String：H04 预填任务标题（如 D05「加入农事日历」） */
    public static final String TASK_TITLE = "task_title";
}
