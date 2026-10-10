package com.heban.app.data.model;

/** P08 物流节点（新 → 旧），如「运输中 · 快件已到达【南京江宁转运中心】，准备派送」。 */
public final class LogisticsNode {
    public final String status;
    public final String text;
    public final long time;

    public LogisticsNode(String status, String text, long time) {
        this.status = status;
        this.text = text;
        this.time = time;
    }
}
