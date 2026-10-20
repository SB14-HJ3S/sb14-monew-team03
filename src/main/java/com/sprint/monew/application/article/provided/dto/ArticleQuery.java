package com.sprint.monew.application.article.provided.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ArticleQuery(
        String keyword,
        UUID interestId,
        List<String> sourceIn,
        LocalDateTime publishDateFrom,
        LocalDateTime publishDateTo,
        String orderBy,
        String direction,
        String cursor,
        LocalDateTime after,
        int limit,
        UUID requestId) {
    public static ArticleQuery of(
            String keyword,
            UUID interestId,
            List<String> sourceIn,
            LocalDateTime publishDateFrom,
            LocalDateTime publishDateTo,
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after,
            int limit,
            UUID requestId) {
        return new ArticleQuery(
                keyword,
                interestId,
                sourceIn,
                publishDateFrom,
                publishDateTo,
                orderBy,
                direction,
                cursor,
                after,
                limit,
                requestId);
    }
}
