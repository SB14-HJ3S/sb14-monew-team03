package com.sprint.monew.adapter.in.controller.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "수정할 댓글 정보")
public record CommentUpdateRequest(
        @Schema(description = "새 댓글 내용 (1~500자)", example = "수정한 댓글 내용입니다.")
                @NotBlank(message = "댓글 내용은 필수입니다.")
                @Size(max = CONTENT_MAX_LENGTH, message = "댓글 내용은 500자 이하여야 합니다.")
                String content) {

    public static final int CONTENT_MAX_LENGTH = 500; // 댓글 내용 최대 글자 수
}
