package com.sprint.monew.application.article.provided.dto;

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

    public static ArticleBackupItem from(ArticleResult article, List<UUID> interestIds) {
        return new ArticleBackupItem(
                article.id(),
                article.source(),
                article.sourceUrl(),
                article.title(),
                article.summary(),
                article.publishedAt(),
                article.createdAt(),
                List.copyOf(interestIds));
    }
}
