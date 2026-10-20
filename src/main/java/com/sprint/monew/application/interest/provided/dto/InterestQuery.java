package com.sprint.monew.application.interest.provided.dto;

import com.sprint.monew.global.pagination.SortDirection;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

public record InterestQuery(
        String keyword,
        @NotNull InterestQuery.SortOrder orderBy,
        @NotNull SortDirection direction,
        String cursor,
        LocalDateTime after,
        @NotNull Integer limit,
        @NotNull UUID requestId) {
    public static InterestQuery of(
            String keyword,
            InterestQuery.SortOrder orderBy,
            SortDirection direction,
            String cursor,
            LocalDateTime after,
            int limit,
            UUID requestId) {
        return new InterestQuery(keyword, orderBy, direction, cursor, after, limit, requestId);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public enum SortOrder {
        NAME("name"),
        SUBSCRIBER_COUNT("subscriberCount"),
        ;
        String value;
    }
}
