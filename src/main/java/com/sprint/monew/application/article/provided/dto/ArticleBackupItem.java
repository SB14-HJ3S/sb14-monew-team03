package com.sprint.monew.application.article.provided.dto;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ArticleBackupItem(
        UUID id,
        ArticleSourceType source,
        String sourceUrl,
        String title,
        String summary,
        LocalDateTime publishedAt,
        Instant createdAt, // TODO: LocalDateTime? Instant?
        List<UUID> interestIds) {
    public static ArticleBackupItem from(Article article, List<UUID> interestIds) {
        return new ArticleBackupItem(
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getSummary(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                List.copyOf(interestIds));
    }
}
