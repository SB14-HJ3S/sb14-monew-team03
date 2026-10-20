package com.sprint.monew.application.article.provided.dto;

import com.sprint.monew.global.pagination.SortDirection;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

public record ArticleQuery(
        String keyword,
        UUID interestId,
        List<String> sourceIn,
        LocalDateTime publishDateFrom,
        LocalDateTime publishDateTo,
        @NotNull ArticleQuery.SortOrder orderBy,
        @NotNull SortDirection direction,
        String cursor,
        LocalDateTime after,
        @NotNull Integer limit,
        @NotNull UUID requestId) {
    public static ArticleQuery of(
            String keyword,
            UUID interestId,
            List<String> sourceIn,
            LocalDateTime publishDateFrom,
            LocalDateTime publishDateTo,
            ArticleQuery.SortOrder orderBy,
            SortDirection direction,
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

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public enum SortOrder {
        PUBLISH_DATE("publishDate"),
        COMMENT_COUNT("commentCount"),
        VIEW_COUNT("viewCount"),
        ;
        String value;
    }
}
