package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewActivityDto(
        UUID id,
        UUID viewedBy,
        Instant createdAt,
        UUID articleId,
        ArticleSourceType source,
        String sourceUrl,
        String articleTitle,
        Instant articlePublishedDate,
        String articleSummary,
        long articleCommentCount,
        long articleViewCount) {
    public static ArticleViewActivityDto from(ArticleViewActivityResult r) {
        return new ArticleViewActivityDto(
                r.id(),
                r.viewedBy(),
                r.createdAt(),
                r.articleId(),
                r.source(),
                r.sourceUrl(),
                r.articleTitle(),
                r.articlePublishedDate(),
                r.articleSummary(),
                r.articleCommentCount(),
                r.articleViewCount());
    }
}
