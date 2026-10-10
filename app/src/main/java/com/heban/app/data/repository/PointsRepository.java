package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.PointsTxnEntity;
import com.heban.app.data.model.CheckInState;
import com.heban.app.data.model.PointsSummary;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 积分与签到（H05、P01、M01；技术准备文档 12.7）。所有加减只追加 points_txn 流水。按当前账号隔离。
 * 游客 / 未登录：observe* 发射 0 值，写操作抛出 NOT_LOGGED_IN。
 */
public interface PointsRepository {
    Flowable<PointsSummary> observeSummary();

    Flowable<Integer> observeBalance();

    Flowable<CheckInState> observeCheckIn();

    /** 签到：一天一次（重复签到抛出 BusinessException("ALREADY_CHECKED_IN", "今天已签到")）；返回获得的积分 */
    Single<Integer> checkIn();

    /**
     * 按规则奖励（type 取 PointsRules.TYPE_POST / ANSWER / FARM_TASK / LEDGER）：检查每日次数和每日 100 上限，
     * 返回实际获得的积分（达到上限或次数时为 0，不报错）。
     */
    Single<Integer> award(String type, String title, @Nullable String refId);

    /** 悬赏被采纳等不计入上限的奖励 */
    Completable grant(String type, String title, int points, int growth, @Nullable String refId);

    /** 消耗积分（置顶、悬赏、兑换）；余额不足抛出 BusinessException("POINTS_NOT_ENOUGH", "积分不足") */
    Completable spend(String type, String title, int points, @Nullable String refId);

    /** 同步版本，供其他 Repository 在 runInTransaction 中调用；余额不足抛出同上异常 */
    void spendInTransaction(long userId, String type, String title, int points, @Nullable String refId);

    /** P01 明细：近 3 个月；filter = all / gain / use */
    Flowable<List<PointsTxnEntity>> observeTxns(String filter);

    /** H05 每日任务：今天某类奖励已获得的次数 */
    Flowable<Integer> observeTodayCount(String type);
}
