package com.sprint.monew.adapter.in.controller.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "댓글 목록 페이지 응답")
public record CursorPageResponseCommentDto(
        @Schema(description = "이번 페이지 댓글 목록") List<CommentDto> content,
        @Schema(description = "다음 페이지 커서 (마지막 댓글 ID, 다음 페이지가 없으면 null)") String nextCursor,
        @Schema(description = "다음 보조 커서 (마지막 댓글 작성 시각, 다음 페이지가 없으면 null)") Instant nextAfter,
        @Schema(description = "이번 페이지 댓글 수") int size,
        @Schema(description = "조건에 맞는 전체 댓글 수") long totalElements,
        @Schema(description = "다음 페이지 여부") boolean hasNext) {}
