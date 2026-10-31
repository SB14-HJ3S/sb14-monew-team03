package com.sprint.monew.application.useractivity.provided.dto;

import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewActivityResult(
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
        long articleViewCount) {}
