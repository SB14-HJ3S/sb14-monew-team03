package com.sprint.monew.adapter.in.controller.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentRegisterRequest(
        @NotNull UUID articleId, // 기사 ID
        @NotNull UUID userId, // 요청 사용자 ID
        @NotBlank @Size(min = 1, max = CONTENT_MAX_LENGTH) String content) { // 내용

    public static final int CONTENT_MAX_LENGTH = 500; // 댓글 내용 최대 글자 수
}
