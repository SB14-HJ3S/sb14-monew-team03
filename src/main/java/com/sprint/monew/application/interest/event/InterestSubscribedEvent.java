package com.sprint.monew.application.interest.event;

import com.sprint.monew.application.interest.provided.usecase.dto.SubscriptionResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterestSubscribedEvent(
        UUID subscriptionId,
        UUID userId,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        Instant createdAt) {
    public static InterestSubscribedEvent of(UUID userId, SubscriptionResult result) {
        return new InterestSubscribedEvent(
                result.id(),
                userId,
                result.interestId(),
                result.interestName(),
                result.interestKeywords(),
                result.subscriberCount(),
                result.createdAt());
    }
}
