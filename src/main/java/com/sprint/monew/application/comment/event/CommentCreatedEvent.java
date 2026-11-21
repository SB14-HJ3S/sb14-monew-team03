package com.sprint.monew.application.comment.event;

import com.sprint.monew.application.comment.provided.usecase.dto.CommentResult;
import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID userId,
        String userNickname,
        String content,
        long likeCount,
        Instant createdAt) {
    public static CommentCreatedEvent of(CommentResult result, String articleTitle) {
        return new CommentCreatedEvent(
                result.id(),
                result.articleId(),
                articleTitle,
                result.userId(),
                result.userNickname(),
                result.content(),
                result.likeCount(),
                result.createdAt());
    }
}
