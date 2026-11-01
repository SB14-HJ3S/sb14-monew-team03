package com.sprint.monew.adapter.in.controller.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @Schema(description = "로그인 이메일", example = "user@example.com")
                @NotBlank
                @Email(message = "이메일 형식이 올바르지 않습니다")
                String email,
        @Schema(description = "로그인 비밀번호", example = "monew1234!") @NotBlank String password) {}
