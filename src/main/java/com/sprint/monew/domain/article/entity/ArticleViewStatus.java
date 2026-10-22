package com.sprint.monew.domain.article.entity;

import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "article_view_statuses",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "article_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleViewStatus extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Article article;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    private ArticleViewStatus(User user, Article article, LocalDateTime lastViewedAt) {
        this.user = user;
        this.article = article;
        this.lastViewedAt = lastViewedAt;
    }

    public static ArticleViewStatus create(User user, Article article, LocalDateTime lastViewedAt) {
        return new ArticleViewStatus(user, article, lastViewedAt);
    }

    public void viewArticle(LocalDateTime viewedAt) {
        this.lastViewedAt = viewedAt;
    }
}
