package com.sprint.monew.application.comment.provided.dto;

import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.pagination.SortedPageQuery;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

public record CommentQuery(
        UUID articleId,
        @NotNull CommentQuery.SortOrder orderBy,
        @NotNull SortDirection direction,
        String cursor,
        Instant after,
        @NotNull int limit,
        @NotNull UUID requestId)
        implements SortedPageQuery<Comment, CommentQuery.SortOrder> {
    public static CommentQuery of(
            UUID articleId,
            CommentQuery.SortOrder orderBy,
            SortDirection direction,
            String cursor,
            LocalDateTime after,
            int limit,
            UUID requestId) {
        return new CommentQuery(
                articleId,
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
    public enum SortOrder implements com.sprint.monew.global.pagination.SortOrder<Comment> {
        CREATED_AT("createdAt", comment -> String.valueOf(comment.getCreatedAt())),
        LIKE_COUNT("likeCount", comment -> String.valueOf(comment.getLikeCount())),
        ;
        String value;
        Function<Comment, String> cursorExtractor;

        @Override
        public String cursorToString(Comment cursor) {
            return cursorExtractor.apply(cursor);
        }
    }
}
