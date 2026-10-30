package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionActivityDto(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        Instant createdAt) {
    public static SubscriptionActivityDto from(SubscriptionActivityResult r) {
        return new SubscriptionActivityDto(
                r.id(),
                r.interestId(),
                r.interestName(),
                r.interestKeywords(),
                r.interestSubscriberCount(),
                r.createdAt());
    }
}
