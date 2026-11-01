package com.sprint.monew.adapter.in.controller.user.dto.response;

import com.sprint.monew.application.user.provided.dto.UserResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
        @Schema(description = "사용자 ID") UUID id,
        @Schema(description = "이메일") String email,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "가입한 날짜") Instant createdAt) {

    public static UserDto from(UserResult result) {
        return new UserDto(result.id(), result.email(), result.nickname(), result.createdAt());
    }
}
