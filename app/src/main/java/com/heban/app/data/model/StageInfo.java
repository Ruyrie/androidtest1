package com.heban.app.data.model;

/** 生长阶段步进条中的一个节点（C02）。 */
public final class StageInfo {
    public final String name;
    public final boolean done;
    public final boolean current;

    public StageInfo(String name, boolean done, boolean current) {
        this.name = name;
        this.done = done;
        this.current = current;
    }
}
