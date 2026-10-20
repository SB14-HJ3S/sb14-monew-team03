package com.sprint.monew.domain.comment.entity;

import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "comment_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.SET_NULL) // 지워지면 user만 null, 좋아요는 유지
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    private CommentLike(User user, Comment comment) {
        this.user = user;
        this.comment = comment;
    }

    public static CommentLike create(User user, Comment comment) {
        return new CommentLike(user, comment);
    }
}
