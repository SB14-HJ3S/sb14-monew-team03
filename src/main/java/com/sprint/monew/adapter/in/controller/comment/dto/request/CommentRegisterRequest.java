package com.sprint.monew.adapter.in.controller.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "등록할 댓글 정보")
public record CommentRegisterRequest(
        @Schema(description = "댓글을 달 기사 ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                @NotNull
                UUID articleId,
        @Schema(description = "댓글을 쓰는 사용자 ID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                @NotNull
                UUID userId,
        @Schema(description = "댓글 내용 (1~500자)", example = "유익한 기사네요.")
                @NotBlank
                @Size(min = 1, max = CONTENT_MAX_LENGTH)
                String content) {

    public static final int CONTENT_MAX_LENGTH = 500; // 댓글 내용 최대 글자 수
}
