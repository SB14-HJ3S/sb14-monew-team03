package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.usecase.dto.CommentLikeActivityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "최근 좋아요를 누른 댓글")
public record CommentLikeActivityDto(
        @Schema(description = "좋아요 ID") UUID id,
        @Schema(description = "좋아요한 날짜") Instant createdAt,
        @Schema(description = "댓글 ID") UUID commentId,
        @Schema(description = "기사 ID") UUID articleId,
        @Schema(description = "기사 제목") String articleTitle,
        @Schema(description = "작성자 ID") UUID commentUserId,
        @Schema(description = "작성자 닉네임") String commentUserNickname,
        @Schema(description = "내용") String commentContent,
        @Schema(description = "좋아요 수") Long commentLikeCount,
        @Schema(description = "작성한 날짜") Instant commentCreatedAt) {

    public static CommentLikeActivityDto from(CommentLikeActivityResult r) {
        return new CommentLikeActivityDto(
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
}
