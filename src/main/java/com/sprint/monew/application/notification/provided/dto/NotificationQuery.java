package com.sprint.monew.application.notification.provided.dto;

import com.sprint.monew.global.pagination.PageQuery;
import jakarta.validation.constraints.Min;
import java.time.Instant;

public record NotificationQuery(
        String cursor, Instant after, @Min(value = 1, message = "limit은 1이상이어야 합니다.") int limit)
        implements PageQuery {

    public static NotificationQuery of(String cursor, Instant after, int limit) {
        return new NotificationQuery(cursor, after, limit);
    }
}
