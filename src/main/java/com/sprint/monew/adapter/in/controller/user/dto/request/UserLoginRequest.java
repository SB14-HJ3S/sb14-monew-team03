package com.sprint.monew.adapter.in.controller.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank @Email(message = "이메일 형식이 올바르지 않습니다") String email, @NotBlank String password) {}
