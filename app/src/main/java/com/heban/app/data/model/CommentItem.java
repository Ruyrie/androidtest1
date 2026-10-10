package com.heban.app.data.model;

import com.heban.app.data.local.entity.CommentEntity;

import java.util.Objects;

/** 评论 / 回答列表项（S02 / S05）。 */
public final class CommentItem {
    public CommentEntity comment;
    public Author author;
    /** 动态 / 问题作者的显示名（用于「草莓姐姐 作者：…」回复） */
    public String postAuthorName;
    public boolean liked;
    /** 所属动态 / 问题的标题或内容摘要（S06「回答了：…」） */
    public String postTitle;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentItem)) return false;
        CommentItem i = (CommentItem) o;
        return liked == i.liked && comment != null && i.comment != null && comment.id == i.comment.id
                && comment.likes == i.comment.likes && comment.accepted == i.comment.accepted
                && Objects.equals(comment.content, i.comment.content)
                && Objects.equals(comment.authorReply, i.comment.authorReply)
                && Objects.equals(author, i.author) && Objects.equals(postAuthorName, i.postAuthorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment == null ? 0 : comment.id, liked);
    }
}
