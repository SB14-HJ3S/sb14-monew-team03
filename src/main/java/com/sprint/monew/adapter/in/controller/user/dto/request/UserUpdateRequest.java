package com.sprint.monew.adapter.in.controller.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "수정할 사용자 정보")
public record UserUpdateRequest(
        @Schema(description = "수정 닉네임", example = "새닉네임")
                @NotBlank
                @Size(min = 1, max = NICKNAME_MAX_LENGTH)
                String nickname) {

    public static final int NICKNAME_MAX_LENGTH = 20; // 닉네임 최대 글자 수
}
