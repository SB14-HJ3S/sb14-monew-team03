package com.sprint.monew.adapter.out.repository.useractivity.document;

import com.sprint.monew.application.useractivity.provided.usecase.dto.CommentActivityResult;
import java.time.Instant;
import java.util.UUID;

public record CommentItem(
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID userId,
        String userNickname,
        String content,
        long likeCount,
        Instant createdAt) {

    public static CommentItem from(CommentActivityResult r) {
        return new CommentItem(
                r.id(),
                r.articleId(),
                r.articleTitle(),
                r.userId(),
                r.userNickname(),
                r.content(),
                r.likeCount(),
                r.createdAt());
    }

    public CommentActivityResult toResult() {
        return new CommentActivityResult(
                commentId,
                articleId,
                articleTitle,
                userId,
                userNickname,
                content,
                likeCount,
                createdAt);
    }
}
