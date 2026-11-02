package com.sprint.monew.application.article.provided.dto;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;

public record CollectedArticle(
        ArticleSourceType source,
        String sourceUrl,
        String title,
        LocalDateTime publishedAt,
        String summary) {

    public Article toArticle() {
        return Article.create(source, sourceUrl, title, publishedAt, summary);
    }
}
