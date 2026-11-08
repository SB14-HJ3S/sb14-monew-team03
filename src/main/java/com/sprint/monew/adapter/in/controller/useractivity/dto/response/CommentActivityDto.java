package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.usecase.dto.CommentActivityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "최근 작성한 댓글")
public record CommentActivityDto(
        @Schema(description = "댓글 ID") UUID id,
        @Schema(description = "기사 ID") UUID articleId,
        @Schema(description = "기사 제목") String articleTitle,
        @Schema(description = "작성자 ID") UUID userId,
        @Schema(description = "작성자 닉네임") String userNickname,
        @Schema(description = "댓글 내용") String content,
        @Schema(description = "좋아요 수") long likeCount,
        @Schema(description = "작성한 날짜") Instant createdAt) {

    public static CommentActivityDto from(CommentActivityResult r) {
        return new CommentActivityDto(
                r.id(),
                r.articleId(),
                r.articleTitle(),
                r.userId(),
                r.userNickname(),
                r.content(),
                r.likeCount(),
                r.createdAt());
    }
}
