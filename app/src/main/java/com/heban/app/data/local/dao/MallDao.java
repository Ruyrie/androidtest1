package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.AddressEntity;
import com.heban.app.data.local.entity.MallItemEntity;
import com.heban.app.data.local.entity.MallOrderEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 积分商城：商品、订单、收货地址。 */
@Dao
public interface MallDao {
    @Query("SELECT * FROM mall_item ORDER BY id")
    Flowable<List<MallItemEntity>> observeItems();

    @Query("SELECT * FROM mall_item WHERE id = :id")
    Flowable<MallItemEntity> observeItem(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItemsSync(List<MallItemEntity> items);

    @Query("SELECT * FROM mall_order WHERE user_id = :userId ORDER BY created_at DESC")
    Flowable<List<MallOrderEntity>> observeOrders(long userId);

    @Query("SELECT * FROM mall_order WHERE order_no = :orderNo")
    Flowable<MallOrderEntity> observeOrder(String orderNo);

    @Update
    Completable updateOrder(MallOrderEntity order);

    @Query("SELECT * FROM address WHERE user_id = :userId ORDER BY is_default DESC, created_at")
    Flowable<List<AddressEntity>> observeAddresses(long userId);

    @Insert
    Single<Long> insertAddress(AddressEntity address);

    @Update
    Completable updateAddress(AddressEntity address);

    @Query("DELETE FROM address WHERE id = :id")
    Completable deleteAddress(long id);
}
