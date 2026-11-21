package com.sprint.monew.adapter.in.controller.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "수정할 사용자 정보")
public record UserUpdateRequest(
        @Schema(description = "수정 닉네임", example = "새닉네임")
                @NotBlank(message = "닉네임을 입력해 주세요")
                @Size(max = NICKNAME_MAX_LENGTH, message = "닉네임은 20자 이하여야 합니다")
                String nickname) {

    public static final int NICKNAME_MAX_LENGTH = 20; // 닉네임 최대 글자 수
}
