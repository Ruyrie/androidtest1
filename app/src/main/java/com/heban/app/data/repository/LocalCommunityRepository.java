package com.heban.app.data.repository;

import android.content.Context;

import androidx.paging.PagingData;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.DraftEntity;
import com.heban.app.data.local.entity.PostEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.CommentItem;
import com.heban.app.data.model.FarmerCard;
import com.heban.app.data.model.PostCard;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/** CommunityRepository 的本地实现（Room + 模拟数据）。M6 数据层实现前为占位。 */
public final class LocalCommunityRepository implements CommunityRepository {
    private final Context app;
    private final AppDatabase db;
    private final CurrentUser user;
    private final Gson gson;
    private final UserRepository userRepository;
    private final PointsRepository points;
    private final AiRepository ai;
    private final MessageRepository messages;

    public LocalCommunityRepository(Context app, AppDatabase db, CurrentUser user, Gson gson,
                                    UserRepository userRepository, PointsRepository points,
                                    AiRepository ai, MessageRepository messages) {
        this.app = app;
        this.db = db;
        this.user = user;
        this.gson = gson;
        this.userRepository = userRepository;
        this.points = points;
        this.ai = ai;
        this.messages = messages;
    }

    @Override
    public Flowable<PagingData<PostCard>> feed(String tab) {
        return Flowable.just(PagingData.empty());
    }

    @Override
    public Flowable<List<PostCard>> questions(String filter) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<PostCard>> search(String keyword) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<PostCard> observePost(long postId) {
        return Flowable.empty();
    }

    @Override
    public Completable addView(long postId) {
        return Completable.complete();
    }

    @Override
    public Flowable<List<CommentItem>> observeComments(long postId, String sort) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Completable toggleLike(long postId) {
        return Completable.complete();
    }

    @Override
    public Completable toggleCommentLike(long commentId) {
        return Completable.complete();
    }

    @Override
    public Completable toggleFavorite(long postId) {
        return Completable.complete();
    }

    @Override
    public Completable toggleFollow(long farmerId) {
        return Completable.complete();
    }

    @Override
    public Single<Long> comment(long postId, String text) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Single<Long> answer(long questionId, String text) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable accept(long questionId, long answerId) {
        return Completable.complete();
    }

    @Override
    public Single<Long> publish(PostEntity post, boolean aiAnswer) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable deletePost(long postId) {
        return Completable.complete();
    }

    @Override
    public Maybe<DraftEntity> getDraft(String type) {
        return Maybe.empty();
    }

    @Override
    public Completable saveDraft(DraftEntity draft) {
        return Completable.complete();
    }

    @Override
    public Completable deleteDraft(String type) {
        return Completable.complete();
    }

    @Override
    public List<String> topics() {
        return Collections.emptyList();
    }

    @Override
    public Flowable<FarmerCard> observeFarmer(long farmerId) {
        return Flowable.empty();
    }

    @Override
    public Flowable<List<PostCard>> postsBy(long farmerId, String type) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<CommentItem>> answersBy(long farmerId) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<FarmerCard>> following() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<FarmerCard>> fans() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<int[]> myStats() {
        return Flowable.just(new int[4]);
    }

    @Override
    public Completable refundExpiredRewards() {
        return Completable.complete();
    }

    @Override
    public Flowable<List<PostCard>> favorites() {
        return Flowable.just(Collections.emptyList());
    }
}
