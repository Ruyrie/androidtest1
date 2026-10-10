package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.AddressEntity;
import com.heban.app.data.local.entity.MallItemEntity;
import com.heban.app.data.local.entity.MallOrderEntity;
import com.heban.app.data.model.LogisticsNode;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 积分商城（P03–P08，技术准备文档 12.7）。订单与地址按当前账号保存。 */
public interface MallRepository {
    String TAB_ALL = "all";
    String TAB_WAIT = "wait";
    String TAB_SHIP = "ship";
    String TAB_DONE = "done";

    /** type = 全部 / 实物 / 装扮 */
    Flowable<List<MallItemEntity>> observeItems(String type);

    Flowable<MallItemEntity> observeItem(long itemId);

    /** 当前账号已兑换该商品的件数（每人限兑） */
    Flowable<Integer> observeRedeemedQty(long itemId);

    /**
     * P05 确认兑换：同一事务写入积分流水（−积分）、订单、库存减少；实物需要 addressId。
     * 积分不足 / 超出限兑 / 库存不足抛出 BusinessException。返回订单。
     */
    Single<MallOrderEntity> redeem(long itemId, int qty, @Nullable Long addressId);

    /** P07 兑换记录，tab = TAB_* */
    Flowable<List<MallOrderEntity>> observeOrders(String tab);

    Flowable<MallOrderEntity> observeOrder(String orderNo);

    /** P08 物流节点（新 → 旧），按订单创建时间偏移生成 */
    List<LogisticsNode> logistics(MallOrderEntity order, long now);

    /** 按当前时间计算的订单状态（MallOrderEntity.STATUS_*） */
    String statusAt(MallOrderEntity order, long now);

    /** 预计送达 / 发货时间（毫秒） */
    long estimatedAt(MallOrderEntity order);

    /** P07「修改地址」：仅待发货订单 */
    Completable changeOrderAddress(String orderNo, long addressId);

    Flowable<List<AddressEntity>> observeAddresses();

    /** 新增或更新；第一个地址自动设为默认；最多 10 个 */
    Single<Long> saveAddress(AddressEntity address);

    Completable deleteAddress(long addressId);

    Completable setDefaultAddress(long addressId);
}
