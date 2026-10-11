package com.heban.app.ui.home;

import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.TaskItem;
import com.heban.app.data.model.WeatherState;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** H01 / H02 单元测试用的假 Repository：只实现用到的方法，记录调用。 */
final class HomeFakes {
    private HomeFakes() {
    }

    /** 未处理的方法返回 Completable.complete() / Flowable.never() / Single.never() */
    @SuppressWarnings("unchecked")
    static <T> T fake(Class<T> type, List<String> calls, BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, args) -> {
            if (m.getDeclaringClass() == Object.class) {
                if (m.getName().equals("equals")) return p == args[0];
                if (m.getName().equals("hashCode")) return System.identityHashCode(p);
                return type.getSimpleName();
            }
            StringBuilder sb = new StringBuilder(m.getName());
            if (args != null) {
                for (Object a : args) sb.append(':').append(a);
            }
            calls.add(sb.toString());
            Object r = handler.apply(m.getName(), args);
            if (r != null) return r;
            Class<?> rt = m.getReturnType();
            if (rt == Completable.class) return Completable.complete();
            if (rt == Flowable.class) return Flowable.never();
            if (rt == Single.class) return Single.never();
            if (rt == boolean.class) return false;
            return null;
        });
    }

    static TaskItem task(long id, String title, boolean done) {
        TaskItem t = new TaskItem();
        t.id = id;
        t.title = title;
        t.done = done;
        return t;
    }

    static CropCard crop(long id, String name) {
        CropCard c = new CropCard();
        c.id = id;
        c.name = name;
        return c;
    }

    static List<TaskItem> tasks(TaskItem... items) {
        List<TaskItem> list = new ArrayList<>();
        for (TaskItem t : items) list.add(t);
        return list;
    }

    static WeatherState loading() {
        WeatherState s = new WeatherState();
        s.loading = true;
        return s;
    }
}
