package com.sprint.monew.application.user.provided.usecase.dto;

import com.sprint.monew.domain.user.entity.User;
import java.time.Instant;
import java.util.UUID;

public record UserResult(UUID id, String email, String nickname, Instant createdAt) {

    public static UserResult from(User user) {
        return new UserResult(
                user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt());
    }
}
