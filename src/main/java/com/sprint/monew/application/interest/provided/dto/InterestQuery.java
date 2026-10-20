package com.sprint.monew.application.interest.provided.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterestQuery(
        String keyword,
        String orderBy,
        String direction,
        String cursor,
        LocalDateTime after,
        int limit,
        UUID requestId) {
    public static InterestQuery of(
            String keyword,
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after,
            int limit,
            UUID requestId) {
        return new InterestQuery(keyword, orderBy, direction, cursor, after, limit, requestId);
    }
}
