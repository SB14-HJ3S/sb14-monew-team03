package com.sprint.monew.domain.comment.entity;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.entity.BaseSoftDeletableEntity;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 수정할 때 바뀐 컬럼만 저장해서 그 사이 바뀐 좋아요 수나 삭제 상태를 덮어쓰지 않게 한다
@Entity
@Table(name = "comments")
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseSoftDeletableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Article article;

    @Column(name = "content", nullable = false, length = 500)
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
    public void changeContent(UUID requestUserId, String content) {
        if (isDeleted()) {
            throw new MonewException(CommentErrorType.COMMENT_NOT_FOUND);
        }
        if (!isAuthor(requestUserId)) {
            throw new MonewException(CommentErrorType.COMMENT_NOT_OWNER);
        }
        this.content = content;
    }

    public boolean isAuthor(UUID userId) {
        return user.getId().equals(userId);
    }
}
