package com.sprint.monew.adapter.out.repository.useractivity.document;

import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeItem(
        UUID likeId,
        Instant createdAt,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt) {

    public static CommentLikeItem from(CommentLikeActivityResult r) {
        return new CommentLikeItem(
                r.id(),
                r.createdAt(),
                r.commentId(),
                r.articleId(),
                r.articleTitle(),
                r.commentUserId(),
                r.commentUserNickname(),
                r.commentContent(),
                r.commentLikeCount(),
                r.commentCreatedAt());
    }

    public CommentLikeActivityResult toResult() {
        return new CommentLikeActivityResult(
                likeId,
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
