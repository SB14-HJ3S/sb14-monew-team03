package com.sprint.monew.adapter.in.controller.comment.dto.response;

import com.sprint.monew.application.comment.provided.usecase.dto.CommentLikeResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeDto(
        @Schema(description = "좋아요 ID") UUID id,
        @Schema(description = "좋아요를 누른 사용자 ID") UUID likedBy,
        @Schema(description = "좋아요를 누른 시각") Instant createdAt,
        @Schema(description = "댓글 ID") UUID commentId,
        @Schema(description = "기사 ID") UUID articleId,
        @Schema(description = "댓글 작성자 ID") UUID commentUserId,
        @Schema(description = "댓글 작성자 닉네임") String commentUserNickname,
        @Schema(description = "댓글 내용") String commentContent,
        @Schema(description = "이번 좋아요를 반영한 좋아요 수") Long commentLikeCount,
        @Schema(description = "댓글 작성 시각") Instant commentCreatedAt) {

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
