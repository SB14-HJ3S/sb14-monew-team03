package com.sprint.monew.adapter.in.controller.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotBlank @Email(message = "이메일 형식이 올바르지 않습니다") @Size(max = EMAIL_MAX_LENGTH)
                String email, // 가입 이메일
        @NotBlank @Size(min = 1, max = NICKNAME_MAX_LENGTH) String nickname, // 가입 닉네임
        @NotBlank
                @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
                @Pattern(regexp = PASSWORD_PATTERN, message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다")
                String password) { // 가입 비밀번호

    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int NICKNAME_MAX_LENGTH = 20; // 닉네임 최대 글자 수
    public static final int PASSWORD_MIN_LENGTH = 6; // 비밀번호 최소 글자 수
    public static final int PASSWORD_MAX_LENGTH = 20; // 비밀번호 최대 글자 수
    public static final String PASSWORD_PATTERN =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$";
}
