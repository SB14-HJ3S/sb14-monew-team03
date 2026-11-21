package com.sprint.monew.adapter.out.repository.useractivity.document;

import com.sprint.monew.application.useractivity.provided.usecase.dto.ArticleViewActivityResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewItem(
        UUID viewId,
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

    public static ArticleViewItem from(ArticleViewActivityResult r) {
        return new ArticleViewItem(
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

    public ArticleViewActivityResult toResult() {
        return new ArticleViewActivityResult(
                viewId,
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
