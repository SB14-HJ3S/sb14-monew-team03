package com.sprint.monew.application.comment.provided.dto;

import com.sprint.monew.global.pagination.SortDirection;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

public record CommentQuery(
        UUID articleId,
        @NotNull CommentQuery.SortOrder orderBy,
        @NotNull SortDirection direction,
        String cursor,
        LocalDateTime after,
        @NotNull Integer limit,
        @NotNull UUID requestId) {
    public static CommentQuery of(
            UUID articleId,
            CommentQuery.SortOrder orderBy,
            SortDirection direction,
            String cursor,
            LocalDateTime after,
            int limit,
            UUID requestId) {
        return new CommentQuery(articleId, orderBy, direction, cursor, after, limit, requestId);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public enum SortOrder {
        CREATED_AT("createdAt"),
        DIRECTION("direction"),
        ;
        String value;
    }
}
