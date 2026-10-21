package com.sprint.monew.application.interest.provided.dto;

import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.pagination.SortedPageQuery;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
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
        Instant after,
        @NotNull int limit,
        @NotNull UUID requestId)
        implements SortedPageQuery<InterestQuery.SortOrder> {
    public static InterestQuery of(
            String keyword,
            InterestQuery.SortOrder orderBy,
            SortDirection direction,
            String cursor,
            LocalDateTime after,
            int limit,
            UUID requestId) {
        return new InterestQuery(
                keyword,
                orderBy,
                direction,
                cursor,
                Objects.nonNull(after) ? after.atZone(ZoneId.systemDefault()).toInstant() : null,
                limit,
                requestId);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public enum SortOrder implements com.sprint.monew.global.pagination.SortOrder {
        NAME("name"),
        SUBSCRIBER_COUNT("subscriberCount"),
        ;
        String value;
    }
}
