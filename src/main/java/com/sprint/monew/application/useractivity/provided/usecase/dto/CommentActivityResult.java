package com.sprint.monew.application.useractivity.provided.usecase.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentActivityResult(
        UUID id,
        UUID articleId,
        String articleTitle,
        UUID userId,
        String userNickname,
        String content,
        long likeCount,
        Instant createdAt) {}
