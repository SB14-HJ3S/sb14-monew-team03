package com.sprint.monew.adapter.in.controller.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank @Size(min = 1, max = CONTENT_MAX_LENGTH) String content) { // 내용

    public static final int CONTENT_MAX_LENGTH = 500; // 댓글 내용 최대 글자 수
}
