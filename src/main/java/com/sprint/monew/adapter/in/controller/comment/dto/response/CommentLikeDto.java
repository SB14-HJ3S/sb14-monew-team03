package com.sprint.monew.adapter.in.controller.comment.dto.response;

import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeDto(
        UUID id, // 좋아요 ID
        UUID likedBy, // 좋아요한 사용자 ID
        Instant createdAt, // 좋아요한 날짜
        UUID commentId, // 댓글 ID
        UUID articleId, // 기사 ID
        UUID commentUserId, // 작성자 ID
        String commentUserNickname, // 작성자 닉네임
        String commentContent, // 내용
        Long commentLikeCount, // 좋아요 수
        Instant commentCreatedAt) { // 작성된 날짜

    public static CommentLikeDto from(CommentLikeResult result) {
        return new CommentLikeDto(
                result.id(),
                result.likedBy(),
                result.createdAt(),
                result.commentId(),
                result.articleId(),
                result.commentUserId(),
                result.commentUserNickname(),
                result.commentContent(),
                result.commentLikeCount(),
                result.commentCreatedAt());
    }
}
