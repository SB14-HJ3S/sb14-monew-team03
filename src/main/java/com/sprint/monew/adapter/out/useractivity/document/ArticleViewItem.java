package com.sprint.monew.adapter.out.useractivity.document;

import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewItem(
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
    public ArticleViewActivityResult toResult() {
        return new ArticleViewActivityResult(
                id,
                viewedBy,
                createdAt,
                articleId,
                source,
                sourceUrl,
                articleTitle,
                articlePublishedDate,
                articleSummary,
                articleCommentCount,
                articleViewCount);
    }
}
