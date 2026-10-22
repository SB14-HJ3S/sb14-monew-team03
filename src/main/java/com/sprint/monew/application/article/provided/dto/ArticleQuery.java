package com.sprint.monew.application.article.provided.dto;

import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.pagination.SortedPageQuery;
import com.sprint.monew.global.type.ArticleSourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

public record ArticleQuery(
        String keyword,
        UUID interestId,
        List<ArticleSourceType> sourceIn,
        LocalDateTime publishDateFrom,
        LocalDateTime publishDateTo,
        @NotNull ArticleQuery.SortOrder orderBy,
        @NotNull SortDirection direction,
        String cursor,
        Instant after,
        @Min(1) int limit,
        @NotNull UUID requestId)
        implements SortedPageQuery<ArticleQuery.SortOrder> {
    public static ArticleQuery of(
            String keyword,
            UUID interestId,
            List<ArticleSourceType> sourceIn,
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
                Objects.nonNull(after) ? after.atZone(ZoneId.systemDefault()).toInstant() : null,
                limit,
                requestId);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public enum SortOrder implements com.sprint.monew.global.pagination.SortOrder {
        PUBLISH_DATE("publishDate"),
        COMMENT_COUNT("commentCount"),
        VIEW_COUNT("viewCount"),
        ;
        String value;
    }
}
