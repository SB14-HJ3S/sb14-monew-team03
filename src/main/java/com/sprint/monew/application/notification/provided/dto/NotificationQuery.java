package com.sprint.monew.application.notification.provided.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationQuery(String cursor, LocalDateTime after, int limit, UUID requestId) {
    public static NotificationQuery of(
            String cursor, LocalDateTime after, int limit, UUID requestId) {
        return new NotificationQuery(cursor, after, limit, requestId);
    }
}
