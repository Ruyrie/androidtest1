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

/**
 * 集市商品。分页查询（技术准备文档 7.6）：置顶商品（pinned_until > now）始终排在最前，其余按排序方式；
 * 排序与 MarketRules.comparator 一致，最后用 id 保证顺序稳定（LIMIT / OFFSET 分页不重复不遗漏）。
 */
@Dao
public interface ProductDao {
    /** 技术准备文档 7.6：置顶商品（pinned_until > now）排在最前；其余「最新发布」 */
    @Query("SELECT * FROM product WHERE (:category IS NULL OR category = :category) "
            + "ORDER BY (pinned_until > :now) DESC, created_at DESC, id DESC")
    PagingSource<Integer, ProductEntity> pagingSource(@Nullable String category, long now);

    /** 置顶在前，其余「距离最近」（自己发布的距离为 0） */
    @Query("SELECT * FROM product WHERE (:category IS NULL OR category = :category) "
            + "ORDER BY (pinned_until > :now) DESC, distance_km ASC, created_at DESC, id DESC")
    PagingSource<Integer, ProductEntity> pagingByDistance(@Nullable String category, long now);

    /** 置顶在前，其余「价格最低」 */
    @Query("SELECT * FROM product WHERE (:category IS NULL OR category = :category) "
            + "ORDER BY (pinned_until > :now) DESC, price_cent ASC, created_at DESC, id DESC")
    PagingSource<Integer, ProductEntity> pagingByPrice(@Nullable String category, long now);

    @Query("SELECT COUNT(*) FROM product WHERE (:category IS NULL OR category = :category)")
    Flowable<Integer> observeCount(@Nullable String category);

    /** D01「附近好物」：不含自己发布的，距离最近的 limit 件 */
    @Query("SELECT * FROM product WHERE mine = 0 ORDER BY distance_km ASC, created_at DESC, id DESC LIMIT :limit")
    Flowable<List<ProductEntity>> observeNearby(int limit);

    /** M01「我的发布」 */
    @Query("SELECT * FROM product WHERE user_id = :userId AND mine = 1 ORDER BY created_at DESC, id DESC")
    Flowable<List<ProductEntity>> observeMine(long userId);

    @Query("SELECT * FROM product WHERE id = :id")
    Flowable<ProductEntity> observe(long id);

    @Query("SELECT * FROM product WHERE id = :id")
    Single<ProductEntity> get(long id);

    @Query("UPDATE product SET views = views + 1 WHERE id = :id")
    Completable addView(long id);

    @Insert
    Single<Long> insert(ProductEntity product);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<ProductEntity> products);

    @Update
    Completable update(ProductEntity product);

    @Query("DELETE FROM product WHERE id = :id")
    Completable delete(long id);

    // —— 同步版本，供 Repository / MockDataImporter 在事务中使用 ——

    @Insert
    long insertSync(ProductEntity product);

    /** 只删除该账号自己发布的商品，返回删除行数 */
    @Query("DELETE FROM product WHERE id = :id AND user_id = :userId AND mine = 1")
    int deleteMineSync(long id, long userId);

    /** 所有账号自己发布的商品 id（导入模拟数据时跳过） */
    @Query("SELECT id FROM product WHERE mine = 1")
    List<Long> mineIdsSync();

    /** 共享（模拟）商品数量 */
    @Query("SELECT COUNT(*) FROM product WHERE mine = 0")
    int countSharedSync();
}
