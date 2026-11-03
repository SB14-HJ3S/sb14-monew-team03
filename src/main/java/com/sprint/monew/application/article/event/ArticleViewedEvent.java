package com.sprint.monew.application.article.event;

import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
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
        Instant viewedAt) {}
