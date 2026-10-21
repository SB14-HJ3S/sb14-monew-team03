package com.sprint.monew.application.article.provided.dto;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleListItem(
        UUID id,
        ArticleSourceType source,
        String sourceUrl,
        String title,
        LocalDateTime publishedAt,
        String summary,
        long commentCount,
        long viewCount,
        boolean viewedByMe) {

    public static ArticleListItem from(Article article, boolean viewedByMe) {

        return new ArticleListItem(
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
