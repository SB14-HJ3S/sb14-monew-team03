package com.sprint.monew.adapter.in.controller.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(min = 1, max = NICKNAME_MAX_LENGTH) String nickname) { // 수정 닉네임

    public static final int NICKNAME_MAX_LENGTH = 20; // 닉네임 최대 글자 수
}
