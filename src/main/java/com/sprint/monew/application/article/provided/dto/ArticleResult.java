package com.sprint.monew.application.article.provided.dto;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleResult(
        UUID id,
        ArticleSourceType source,
        String sourceUrl,
        String title,
        LocalDateTime publishedAt,
        String summary,
        long commentCount,
        long viewCount,
        boolean viewedByMe) {

    public static ArticleResult from(Article article, boolean viewedByMe) {

        return new ArticleResult(
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishedAt(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount(),
                viewedByMe);
    }
}
