package com.sprint.monew.application.article.provided.dto;

import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;

public record CollectedArticle(
        ArticleSourceType source,
        String sourceUrl,
        String title,
        LocalDateTime publishedAt,
        String summary) {}
