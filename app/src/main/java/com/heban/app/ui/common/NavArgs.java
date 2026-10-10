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

    /** long：病虫害百科 id（D05） */
    public static final String PEST_ID = "pest_id";
    /** long：集市商品 id（D08） */
    public static final String PRODUCT_ID = "product_id";
    /** long：识别记录 id（D03 打开历史记录） */
    public static final String SCAN_RECORD_ID = "scan_record_id";
    /** long：AI 会话 id（D06 打开历史会话） */
    public static final String SESSION_ID = "session_id";
    /** String：D06 进入后自动发送的第一条问题（D03 / D05「问 AI」带入识别结论或病害名称） */
    public static final String AI_PROMPT = "ai_prompt";
    /** long：记账条目 id（D11 编辑） */
    public static final String ENTRY_ID = "entry_id";
    /** String：LedgerEntryEntity.TYPE_IN / TYPE_OUT（D11 预选收支） */
    public static final String ENTRY_TYPE = "entry_type";

    /** long：动态 / 提问 id（S02、S05） */
    public static final String POST_ID = "post_id";
    /** long：农友 id（S06、S09） */
    public static final String FARMER_ID = "farmer_id";
    /** long：私信会话 id（S09、P09） */
    public static final String CONVERSATION_ID = "conversation_id";
    /** String：S03 发布模式 PostEntity.TYPE_POST / TYPE_QUESTION */
    public static final String PUBLISH_TYPE = "publish_type";
    /** String：S03 预填内容（如 D03「发到问答」带入识别结论） */
    public static final String PUBLISH_TEXT = "publish_text";
    /** String：S07 打开的 Tab：fans / following */
    public static final String FOLLOWS_TAB = "follows_tab";
    /** long：积分商品 id（P04 / P05） */
    public static final String MALL_ITEM_ID = "mall_item_id";
    /** int：兑换数量（P05） */
    public static final String QTY = "qty";
    /** String：兑换订单号（P08 物流、P07 修改地址） */
    public static final String ORDER_NO = "order_no";
    /** boolean：P06 以「选择地址」模式打开（选中后返回 P05） */
    public static final String PICK_ADDRESS = "pick_address";
}
