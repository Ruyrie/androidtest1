package com.heban.app.testutil;

import com.heban.app.data.local.dao.AppealDao;
import com.heban.app.data.local.entity.AppealEntity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 内存版 appeal 表 */
public class FakeAppealDao implements AppealDao {
    public final List<AppealEntity> rows = new ArrayList<>();

    @Override
    public Single<Long> insert(AppealEntity appeal) {
        return Single.fromCallable(() -> {
            rows.add(appeal);
            return (long) rows.size();
        });
    }

    @Override
    public Flowable<AppealEntity> observe(long id) {
        return Flowable.fromIterable(rows).filter(a -> a.id == id);
    }
}
