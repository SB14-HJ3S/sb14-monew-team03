package com.sprint.monew.domain.article.entity;

import com.sprint.monew.global.entity.BaseSoftDeletableEntity;
import com.sprint.monew.global.type.ArticleSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Table(
        name = "articles",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"source_url"})},
        indexes = {
            @Index(name = "idx_articles_published", columnList = "published_at, created_at, id")
        })
@Check(name = "ck_articles_comment_count", constraints = "comment_count >= 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseSoftDeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ArticleSourceType source;

    @Column(name = "source_url", nullable = false, columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "summary", nullable = false, columnDefinition = "text")
    private String summary;

    // 백업에서 복구한 기사는 다시 백업하지 않는다. 일반 수집 기사는 null이다.
    @Column(name = "restored_at", updatable = false)
    private Instant restoredAt;

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

    public static Article restore(
            ArticleSourceType source,
            String sourceUrl,
            String title,
            LocalDateTime publishedAt,
            String summary) {
        Article article = new Article(source, sourceUrl, title, publishedAt, summary);
        article.restoredAt = Instant.now();
        return article;
    }

    public void updateContent(String title, String summary) {
        this.title = title;
        this.summary = summary;
    }
}
