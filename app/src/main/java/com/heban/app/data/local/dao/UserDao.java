package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.UserEntity;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface UserDao {
    @Query("SELECT * FROM user WHERE phone = :phone LIMIT 1")
    Maybe<UserEntity> findByPhone(String phone);

    @Query("SELECT * FROM user WHERE id = :id")
    Single<UserEntity> get(long id);

    @Query("SELECT * FROM user WHERE id = :id")
    Flowable<UserEntity> observe(long id);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    Single<Long> insert(UserEntity user);

    @Update
    Completable update(UserEntity user);
}
