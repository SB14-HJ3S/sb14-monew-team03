package com.sprint.monew.domain.user.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id, // 사용자 ID
        String email, // 이메일
        String nickname, // 닉네임
        Instant createdAt) {} // 가입한 날짜
