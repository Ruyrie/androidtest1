package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.ProductEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ProductDao {
    /** 技术准备文档 7.6：置顶商品（pinned_until > now）排在最前 */
    @Query("SELECT * FROM product WHERE (:category IS NULL OR category = :category) "
            + "ORDER BY (pinned_until > :now) DESC, created_at DESC")
    PagingSource<Integer, ProductEntity> pagingSource(@Nullable String category, long now);

    @Query("SELECT * FROM product WHERE id = :id")
    Flowable<ProductEntity> observe(long id);

    @Query("SELECT * FROM product WHERE id = :id")
    Single<ProductEntity> get(long id);

    @Insert
    Single<Long> insert(ProductEntity product);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<ProductEntity> products);

    @Update
    Completable update(ProductEntity product);

    @Query("DELETE FROM product WHERE id = :id")
    Completable delete(long id);
}
