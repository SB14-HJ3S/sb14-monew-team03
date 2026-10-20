package com.sprint.monew.application.comment.provided.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentQuery(
        UUID articleId,
        String orderBy,
        String direction,
        String cursor,
        LocalDateTime after,
        int limit,
        UUID requestId) {
    public static CommentQuery of(
            UUID articleId,
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after,
            int limit,
            UUID requestId) {
        return new CommentQuery(articleId, orderBy, direction, cursor, after, limit, requestId);
    }
}
