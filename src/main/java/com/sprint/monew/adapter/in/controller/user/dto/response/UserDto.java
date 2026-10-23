package com.sprint.monew.adapter.in.controller.user.dto.response;

import com.sprint.monew.application.user.provided.dto.UserResult;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id, // 사용자 ID
        String email, // 이메일
        String nickname, // 닉네임
        Instant createdAt) { // 가입한 날짜

    public static UserDto from(UserResult result) {
        return new UserDto(result.id(), result.email(), result.nickname(), result.createdAt());
    }
}
