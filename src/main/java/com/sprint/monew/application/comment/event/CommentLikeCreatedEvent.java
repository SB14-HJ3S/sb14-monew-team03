package com.sprint.monew.application.comment.event;

import com.sprint.monew.application.comment.provided.usecase.dto.CommentLikeResult;
import java.time.Instant;
import java.util.UUID;

/** 좋아요 등록 완료 (활동 내역용). 좋아요 누른 사람 문서의 "좋아요한 댓글"에 추가한다. */
public record CommentLikeCreatedEvent(
        UUID likeId,
        UUID likedById,
        Instant likedAt,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt) {
    public static CommentLikeCreatedEvent of(CommentLikeResult result, String articleTitle) {
        return new CommentLikeCreatedEvent(
                result.id(),
                result.likedBy(),
                result.createdAt(),
                result.commentId(),
                result.articleId(),
                articleTitle,
                result.commentUserId(),
                result.commentUserNickname(),
                result.commentContent(),
                result.commentLikeCount(),
                result.commentCreatedAt());
    }
}
