package com.heban.app.data.repository;

import com.heban.app.data.local.dao.TaskDayType;
import com.heban.app.data.local.dao.TaskRow;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.TaskItem;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** 农事任务列表项与日历圆点（纯 Java）。 */
public final class TaskItems {
    private TaskItems() {
    }

    public static TaskItem from(TaskRow r) {
        TaskItem t = new TaskItem();
        t.id = r.id;
        t.title = r.title;
        t.type = FarmType.of(r.type);
        t.cropId = r.cropId;
        t.cropName = r.cropName;
        t.plotName = r.plotName;
        t.dueAt = r.dueAt;
        t.done = r.done;
        t.attention = r.attention;
        return t;
    }

    public static List<TaskItem> from(List<TaskRow> rows) {
        List<TaskItem> out = new ArrayList<>(rows.size());
        for (TaskRow r : rows) out.add(from(r));
        return out;
    }

    /** 每天有哪些农事类型（FarmType.name()，按 FarmType 顺序），日期升序 */
    public static Map<LocalDate, Set<String>> monthTypes(List<TaskDayType> rows, ZoneId zone) {
        Map<LocalDate, EnumSet<FarmType>> days = new TreeMap<>();
        for (TaskDayType r : rows) {
            LocalDate d = DateRanges.toDate(r.dueAt, zone);
            EnumSet<FarmType> set = days.get(d);
            if (set == null) {
                set = EnumSet.noneOf(FarmType.class);
                days.put(d, set);
            }
            set.add(FarmType.of(r.type));
        }
        Map<LocalDate, Set<String>> out = new TreeMap<>();
        for (Map.Entry<LocalDate, EnumSet<FarmType>> e : days.entrySet()) {
            Set<String> names = new LinkedHashSet<>();
            for (FarmType t : e.getValue()) names.add(t.name());
            out.put(e.getKey(), names);
        }
        return out;
    }
}
