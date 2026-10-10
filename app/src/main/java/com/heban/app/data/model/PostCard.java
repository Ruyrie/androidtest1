package com.heban.app.data.model;

import com.heban.app.data.local.entity.PostEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 动态 / 提问卡片（S01、S02、S04、S05、S06、M02、M04）：动态 + 作者 + 当前账号的互动状态。 */
public final class PostCard {
    public PostEntity post;
    public Author author;
    public List<String> images = new ArrayList<>();
    public boolean liked;
    public boolean favorited;
    /** 当前账号已关注作者 */
    public boolean followed;
    /** 回答数（提问） */
    public int answers;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostCard)) return false;
        PostCard c = (PostCard) o;
        return liked == c.liked && favorited == c.favorited && followed == c.followed && answers == c.answers
                && post != null && c.post != null && post.id == c.post.id && post.likes == c.post.likes
                && post.comments == c.post.comments && post.favorites == c.post.favorites
                && Objects.equals(post.status, c.post.status) && Objects.equals(post.content, c.post.content)
                && Objects.equals(author, c.author) && Objects.equals(images, c.images);
    }

    @Override
    public int hashCode() {
        return Objects.hash(post == null ? 0 : post.id, liked, followed);
    }
}
