package com.heban.app.data.repository;

import androidx.paging.PagingData;

import com.heban.app.data.local.entity.DraftEntity;
import com.heban.app.data.local.entity.PostEntity;
import com.heban.app.data.model.CommentItem;
import com.heban.app.data.model.FarmerCard;
import com.heban.app.data.model.PostCard;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * 农友圈（S01–S07，技术准备文档 12.5）。动态、问答、农友来自模拟数据（所有账号共用）；
 * 点赞、关注、收藏、评论、发布、草稿按当前账号保存，未登录时写操作抛出 NOT_LOGGED_IN。
 */
public interface CommunityRepository {
    String TAB_RECOMMEND = "推荐";
    String TAB_FOLLOWING = "关注";
    String TAB_QA = "问答";
    String TAB_LOCAL = "同城";

    String Q_ALL = "全部";
    String Q_OPEN = "待解决";
    String Q_MY_CROPS = "与我作物相关";
    String Q_MINE = "我的提问";

    String SORT_HOT = "最热";
    String SORT_NEW = "最新";

    /** S01 动态流（推荐 / 关注 / 同城；同城按用户资料中的城市），分页 */
    Flowable<PagingData<PostCard>> feed(String tab);

    /** S04 问答列表 */
    Flowable<List<PostCard>> questions(String filter);

    /** S01 搜索动态和问答 */
    Flowable<List<PostCard>> search(String keyword);

    Flowable<PostCard> observePost(long postId);

    Completable addView(long postId);

    /** S02 评论 / S05 回答；sort = 最热 / 最新；S05 中已采纳的排最前 */
    Flowable<List<CommentItem>> observeComments(long postId, String sort);

    /** 点赞采用乐观更新：界面先改，失败回滚 */
    Completable toggleLike(long postId);

    Completable toggleCommentLike(long commentId);

    Completable toggleFavorite(long postId);

    Completable toggleFollow(long farmerId);

    /** S02 发表评论；返回评论 id */
    Single<Long> comment(long postId, String text);

    /** S05 回答问题（按规则奖励积分）；返回回答 id */
    Single<Long> answer(long questionId, String text);

    /** S05 提问者采纳回答（同一事务：回答 accepted、问题 SOLVED；悬赏发给回答者） */
    Completable accept(long questionId, long answerId);

    /**
     * S03 发布动态 / 提问：提问带悬赏时在同一事务内扣除积分；aiAnswer 时发布后生成 AI 参考答案（失败不影响发布）。
     * 发布动态按规则奖励积分。返回动态 id。
     */
    Single<Long> publish(PostEntity post, boolean aiAnswer);

    Completable deletePost(long postId);

    Maybe<DraftEntity> getDraft(String type);

    Completable saveDraft(DraftEntity draft);

    Completable deleteDraft(String type);

    /** S03 话题选项 */
    List<String> topics();

    /** S06 农友主页 */
    Flowable<FarmerCard> observeFarmer(long farmerId);

    /** S06 / M02 动态或问答（type = PostEntity.TYPE_*）；farmerId = 0 表示当前账号自己的 */
    Flowable<List<PostCard>> postsBy(long farmerId, String type);

    /** S06「问答」Tab：该农友的回答 */
    Flowable<List<CommentItem>> answersBy(long farmerId);

    /** S07 我关注的 / 我的粉丝 */
    Flowable<List<FarmerCard>> following();

    Flowable<List<FarmerCard>> fans();

    /** M01 / M02 统计：动态数、关注数、粉丝数、获赞数 */
    Flowable<int[]> myStats();

    /** App 启动时：7 天无人回答的悬赏退回积分 */
    Completable refundExpiredRewards();

    /** 当前账号收藏的动态 / 问答（M04） */
    Flowable<List<PostCard>> favorites();
}
