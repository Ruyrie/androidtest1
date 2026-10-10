package com.heban.app.testutil;

import com.heban.app.data.local.dao.UserDao;
import com.heban.app.data.local.entity.UserEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

/** 内存版 user 表：保存副本（未调用 update 的修改不会生效），phone 唯一，id 自增。 */
public class FakeUserDao implements UserDao {
    public final Map<Long, UserEntity> rows = new LinkedHashMap<>();
    private final BehaviorProcessor<Integer> version = BehaviorProcessor.createDefault(0);
    private long nextId = 1;

    /** 直接放入一行（测试准备数据），返回带 id 的副本 */
    public UserEntity seed(UserEntity u) {
        if (u.id == 0) u.id = nextId++;
        rows.put(u.id, copy(u));
        version.onNext(version.getValue() + 1);
        return copy(u);
    }

    public UserEntity byPhone(String phone) {
        for (UserEntity u : rows.values()) {
            if (u.phone.equals(phone)) return copy(u);
        }
        return null;
    }

    @Override
    public Maybe<UserEntity> findByPhone(String phone) {
        return Maybe.fromCallable(() -> byPhone(phone));
    }

    @Override
    public Single<UserEntity> get(long id) {
        return Single.fromCallable(() -> {
            UserEntity u = rows.get(id);
            if (u == null) throw new IllegalStateException("Query returned empty result set");
            return copy(u);
        });
    }

    @Override
    public Flowable<UserEntity> observe(long id) {
        return version.filter(v -> rows.containsKey(id)).map(v -> copy(rows.get(id)));
    }

    @Override
    public Single<Long> insert(UserEntity user) {
        return Single.fromCallable(() -> {
            if (byPhone(user.phone) != null) {
                throw new IllegalStateException("UNIQUE constraint failed: user.phone");
            }
            long id = user.id != 0 ? user.id : nextId++;
            UserEntity row = copy(user);
            row.id = id;
            rows.put(id, row);
            version.onNext(version.getValue() + 1);
            return id;
        });
    }

    @Override
    public Completable update(UserEntity user) {
        return Completable.fromAction(() -> {
            if (rows.containsKey(user.id)) {
                rows.put(user.id, copy(user));
                version.onNext(version.getValue() + 1);
            }
        });
    }

    public static UserEntity copy(UserEntity s) {
        UserEntity u = new UserEntity();
        u.id = s.id;
        u.phone = s.phone;
        u.passwordHash = s.passwordHash;
        u.salt = s.salt;
        u.prevPasswordHash = s.prevPasswordHash;
        u.prevSalt = s.prevSalt;
        u.nickname = s.nickname;
        u.region = s.region;
        u.identity = s.identity;
        u.crops = s.crops;
        u.bio = s.bio;
        u.years = s.years;
        u.failedAttempts = s.failedAttempts;
        u.lockedUntil = s.lockedUntil;
        u.createdAt = s.createdAt;
        u.deletedAt = s.deletedAt;
        return u;
    }
}
