package com.sprint.monew.application.article.provided.dto;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleViewStatusResult(
        UUID id,
        UUID viewedBy,
        Instant createdAt,
        UUID articleId,
        ArticleSourceType source,
        String sourceUrl,
        String articleTitle,
        LocalDateTime articlePublishedDate,
        String articleSummary,
        long articleCommentCount,
        long articleViewCount) {

    public static ArticleViewStatusResult from(ArticleViewStatus viewStatus) {
        Article article = viewStatus.getArticle();

        return new ArticleViewStatusResult(
                viewStatus.getId(),
                viewStatus.getUser().getId(),
                viewStatus.getCreatedAt(),
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishedAt(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount());
    }
}
