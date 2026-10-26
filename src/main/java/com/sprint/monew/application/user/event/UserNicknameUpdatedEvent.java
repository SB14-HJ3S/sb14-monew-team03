package com.sprint.monew.application.user.event;

import java.util.UUID;

public record UserNicknameUpdatedEvent(UUID userId, String nickname) {}
