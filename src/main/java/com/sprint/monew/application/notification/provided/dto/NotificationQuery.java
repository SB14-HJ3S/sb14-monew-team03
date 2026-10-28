package com.sprint.monew.application.notification.provided.dto;

import com.sprint.monew.global.pagination.PageQuery;
import java.time.Instant;

public record NotificationQuery(String cursor, Instant after, int limit) implements PageQuery {

    public static NotificationQuery of(String cursor, Instant after, int limit) {
        return new NotificationQuery(cursor, after, limit);
    }
}
