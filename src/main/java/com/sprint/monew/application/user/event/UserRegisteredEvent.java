package com.sprint.monew.application.user.event;

import com.sprint.monew.domain.user.entity.User;
import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email, String nickname, Instant createdAt) {

    public static UserRegisteredEvent from(User user) {
        return new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt());
    }
}
