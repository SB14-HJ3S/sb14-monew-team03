package com.sprint.monew.adapter.in.controller.user.dto.request;

import com.sprint.monew.adapter.in.controller.user.dto.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 정보")
public record UserRegisterRequest(
        @Schema(description = "가입 이메일", example = "user@example.com")
                @NotBlank(message = "이메일을 입력해 주세요")
                @Email(message = "이메일 형식이 올바르지 않습니다")
                @Size(max = EMAIL_MAX_LENGTH, message = "이메일은 255자 이하여야 합니다")
                String email,
        @Schema(description = "가입 닉네임", example = "모뉴")
                @NotBlank(message = "닉네임을 입력해 주세요")
                @Size(max = NICKNAME_MAX_LENGTH, message = "닉네임은 20자 이하여야 합니다")
                String nickname,
        @Schema(description = "가입 비밀번호", example = "monew1234!")
                @NotBlank(message = "비밀번호를 입력해 주세요")
                @ValidPassword
                String password) {

    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int NICKNAME_MAX_LENGTH = 20; // 닉네임 최대 글자 수
}
