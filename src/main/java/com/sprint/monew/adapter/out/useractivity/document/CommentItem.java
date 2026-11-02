package com.sprint.monew.adapter.out.useractivity.document;

import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import java.time.Instant;
import java.util.UUID;

public record CommentItem(
        UUID id,
        UUID articleId,
        String articleTitle,
        UUID userId,
        String userNickname,
        String content,
        long likeCount,
        Instant createdAt) {

    public CommentActivityResult toResult() {
        return new CommentActivityResult(
                id, articleId, articleTitle, userId, userNickname, content, likeCount, createdAt);
    }
}
