package com.sprint.monew.adapter.out.useractivity.document;

import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeItem(
        UUID id,
        Instant createdAt,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt) {

    public CommentLikeActivityResult toResult() {
        return new CommentLikeActivityResult(
                id,
                createdAt,
                commentId,
                articleId,
                articleTitle,
                commentUserId,
                commentUserNickname,
                commentContent,
                commentLikeCount,
                commentCreatedAt);
    }
}
