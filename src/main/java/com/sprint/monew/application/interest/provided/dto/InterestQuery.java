package com.sprint.monew.application.interest.provided.dto;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.pagination.SortedPageQuery;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.function.Function;
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
        @NotNull int limit)
        implements SortedPageQuery<Interest, InterestQuery.SortOrder> {
    public static InterestQuery of(
            String keyword,
            InterestQuery.SortOrder orderBy,
            SortDirection direction,
            String cursor,
            Instant after,
            int limit) {
        return new InterestQuery(keyword, orderBy, direction, cursor, after, limit);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public enum SortOrder implements com.sprint.monew.global.pagination.SortOrder<Interest> {
        NAME("name", interest -> String.valueOf(interest.getName())),
        SUBSCRIBER_COUNT(
                "subscriberCount", interest -> String.valueOf(interest.getSubscriberCount())),
        ;
        String value;
        Function<Interest, String> cursorExtractor;

        @Override
        public String cursorToString(Interest cursor) {
            return cursorExtractor.apply(cursor);
        }
    }
}
