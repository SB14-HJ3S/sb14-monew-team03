package com.sprint.monew.application.comment.provided.usecase.dto;

import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.pagination.SortedPageQuery;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

public record CommentQuery(
        UUID articleId,
        @NotNull(message = "정렬 기준은 필수입니다.") CommentQuery.SortOrder orderBy,
        @NotNull(message = "정렬 방향은 필수입니다.") SortDirection direction,
        String cursor,
        Instant after,
        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.") int limit)
        implements SortedPageQuery<Comment> {
    public static CommentQuery of(
            UUID articleId,
            CommentQuery.SortOrder orderBy,
            SortDirection direction,
            String cursor,
            Instant after,
            int limit) {
        return new CommentQuery(articleId, orderBy, direction, cursor, after, limit);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public enum SortOrder implements com.sprint.monew.global.pagination.SortOrder<Comment> {
        CREATED_AT("createdAt"),
        LIKE_COUNT("likeCount"),
        ;
        String value;

        // 커서는 정렬 기준과 상관없이 마지막 댓글의 id (값은 서비스가 그 댓글을 찾아서 꺼냄)
        @Override
        public String extractNextCursorFromLastEntity(Comment lastEntity) {
            return lastEntity.getId().toString();
        }
    }
}
