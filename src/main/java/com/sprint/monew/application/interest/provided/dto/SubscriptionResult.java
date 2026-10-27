package com.sprint.monew.application.interest.provided.dto;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionResult(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        Long subscriberCount,
        Instant createdAt) {

    public static SubscriptionResult of(Interest interest, Subscription created) {
        return new SubscriptionResult(
                created.getId(),
                interest.getId(),
                interest.getName(),
                interest.getKeywordNames(),
                interest.getSubscriberCount(),
                created.getCreatedAt());
    }
}
