package com.sprint.monew.adapter.in.controller.comment.dto.response;

import com.sprint.monew.application.comment.provided.usecase.dto.CommentResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        @Schema(description = "댓글 ID") UUID id,
        @Schema(description = "기사 ID") UUID articleId,
        @Schema(description = "댓글 작성자 ID") UUID userId,
        @Schema(description = "댓글 작성자 닉네임") String userNickname,
        @Schema(description = "댓글 내용") String content,
        @Schema(description = "좋아요 수") Long likeCount,
        @Schema(description = "요청자가 좋아요를 눌렀는지 여부") boolean likedByMe,
        @Schema(description = "댓글 작성 시각") Instant createdAt) {

    public static CommentDto from(CommentResult result) {
        return new CommentDto(
                result.id(),
                result.articleId(),
                result.userId(),
                result.userNickname(),
                result.content(),
                result.likeCount(),
                result.likedByMe(),
                result.createdAt());
    }
}
