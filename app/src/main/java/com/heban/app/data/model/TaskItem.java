package com.heban.app.data.model;

import java.util.Objects;

/** 农事任务列表项（H01「今日农事」、H03 当天农事）。 */
public final class TaskItem {
    public long id;
    public String title;
    public FarmType type;
    public Long cropId;
    public String cropName;
    public String plotName;
    public long dueAt;
    public boolean done;
    public boolean attention;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskItem)) return false;
        TaskItem t = (TaskItem) o;
        return id == t.id && dueAt == t.dueAt && done == t.done && attention == t.attention
                && type == t.type && Objects.equals(title, t.title) && Objects.equals(cropId, t.cropId)
                && Objects.equals(cropName, t.cropName) && Objects.equals(plotName, t.plotName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, done, dueAt);
    }
}
