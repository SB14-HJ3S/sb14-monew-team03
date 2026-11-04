package com.sprint.monew.application.article.event;

import com.sprint.monew.application.article.provided.dto.ArticleViewStatusResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public record ArticleViewedEvent(
        UUID viewId,
        UUID userId,
        UUID articleId,
        ArticleSourceType source,
        String sourceUrl,
        String articleTitle,
        Instant articlePublishedDate,
        String articleSummary,
        long articleCommentCount,
        long articleViewCount,
        Instant viewedAt) {
    public static ArticleViewedEvent of(ArticleViewStatusResult result) {
        return new ArticleViewedEvent(
                result.id(),
                result.viewedBy(),
                result.articleId(),
                result.source(),
                result.sourceUrl(),
                result.articleTitle(),
                result.articlePublishedDate().atZone(ZoneId.systemDefault()).toInstant(),
                result.articleSummary(),
                result.articleCommentCount(),
                result.articleViewCount(),
                result.createdAt());
    }
}
