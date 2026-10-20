package com.sprint.monew.application.notification.provided.dto;

import com.sprint.monew.global.pagination.PageQuery;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public record NotificationQuery(String cursor, Instant after, int limit, UUID requestId)
        implements PageQuery {
    public static NotificationQuery of(
            String cursor, LocalDateTime after, int limit, UUID requestId) {
        return new NotificationQuery(
                cursor,
                Objects.nonNull(after) ? after.atZone(ZoneId.systemDefault()).toInstant() : null,
                limit,
                requestId);
    }
}
