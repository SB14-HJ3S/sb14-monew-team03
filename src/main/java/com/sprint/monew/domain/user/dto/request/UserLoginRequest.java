package com.sprint.monew.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank String email, // 로그인 이메일
        @NotBlank String password) {} // 로그인 비밀번호
