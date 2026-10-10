package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.CommentEntity;
import com.heban.app.data.local.entity.DraftEntity;
import com.heban.app.data.local.entity.FanEntity;
import com.heban.app.data.local.entity.FarmerEntity;
import com.heban.app.data.local.entity.FollowEntity;
import com.heban.app.data.local.entity.LikeEntity;
import com.heban.app.data.local.entity.PostEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/** 农友圈：农友、动态 / 提问、评论 / 回答、关注、点赞、草稿。 */
@Dao
public interface CommunityDao {
    @Query("SELECT * FROM farmer WHERE id = :id")
    Flowable<FarmerEntity> observeFarmer(long id);

    @Query("SELECT * FROM post WHERE id = :id")
    Flowable<PostEntity> observePost(long id);

    @Query("SELECT * FROM comment WHERE post_id = :postId ORDER BY created_at DESC")
    Flowable<List<CommentEntity>> observeComments(long postId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFarmersSync(List<FarmerEntity> farmers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPostsSync(List<PostEntity> posts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCommentsSync(List<CommentEntity> comments);

    @Insert
    Single<Long> insertPost(PostEntity post);

    @Update
    Completable updatePost(PostEntity post);

    @Insert
    Single<Long> insertComment(CommentEntity comment);

    @Update
    Completable updateComment(CommentEntity comment);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertFollow(FollowEntity follow);

    @Query("DELETE FROM follow WHERE user_id = :userId AND farmer_id = :farmerId")
    Completable deleteFollow(long userId, long farmerId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertFan(FanEntity fan);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertLike(LikeEntity like);

    @Query("DELETE FROM like_record WHERE user_id = :userId AND target_type = :type AND target_id = :targetId")
    Completable deleteLike(long userId, String type, long targetId);

    @Query("SELECT * FROM draft WHERE user_id = :userId AND type = :type")
    Maybe<DraftEntity> getDraft(long userId, String type);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable saveDraft(DraftEntity draft);

    @Query("DELETE FROM draft WHERE user_id = :userId AND type = :type")
    Completable deleteDraft(long userId, String type);
}
