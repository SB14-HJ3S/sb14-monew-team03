package com.sprint.monew.domain.article.entity;

import com.sprint.monew.global.entity.BaseSoftDeletableEntity;
import com.sprint.monew.global.type.ArticleSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "articles",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"source_url"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseSoftDeletableEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ArticleSourceType source;

    @Column(name = "source_url", nullable = false, length = 255)
    private String sourceUrl;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "summary", nullable = false, length = 1000)
    private String summary;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    private Article(
            ArticleSourceType source,
            String sourceUrl,
            String title,
            LocalDateTime publishedAt,
            String summary) {
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.title = title;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.viewCount = 0;
        this.commentCount = 0;
    }

    public static Article create(
            ArticleSourceType source,
            String sourceUrl,
            String title,
            LocalDateTime publishedAt,
            String summary) {
        return new Article(source, sourceUrl, title, publishedAt, summary);
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }
}
