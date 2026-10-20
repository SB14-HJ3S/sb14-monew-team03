package com.sprint.monew.domain.comment.entity;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.entity.BaseSoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseSoftDeletableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    private Comment(User user, Article article, String content) {
        this.user = user;
        this.article = article;
        this.content = content;
        this.likeCount = 0L;
    }

    public static Comment create(User user, Article article, String content) {
        return new Comment(user, article, content);
    }

    // 본인이 작성한 댓글만 수정할 수 있다
    public void changeContent(String content) {
        this.content = content;
    }
}
