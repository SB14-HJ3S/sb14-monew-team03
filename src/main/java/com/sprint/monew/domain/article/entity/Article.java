package com.sprint.monew.domain.article.entity;

import com.sprint.monew.global.entity.BaseSoftDeletableEntity;
import com.sprint.monew.global.type.ArticleSourceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "articles",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"source_url"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseSoftDeletableEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ArticleSourceType source;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "summary", nullable = false)
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
}
