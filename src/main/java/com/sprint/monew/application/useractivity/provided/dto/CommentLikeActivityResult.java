package com.sprint.monew.application.useractivity.provided.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeActivityResult(
        UUID id,
        Instant createdAt,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt) {}
