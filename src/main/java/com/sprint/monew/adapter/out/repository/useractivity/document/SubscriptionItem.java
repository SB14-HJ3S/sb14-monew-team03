package com.sprint.monew.adapter.out.repository.useractivity.document;

import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionItem(
        UUID subscriptionId,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        Instant createdAt) {

    public static SubscriptionItem from(SubscriptionActivityResult r) {
        return new SubscriptionItem(
                r.id(),
                r.interestId(),
                r.interestName(),
                r.interestKeywords(),
                r.interestSubscriberCount(),
                r.createdAt());
    }

    public SubscriptionActivityResult toResult() {
        return new SubscriptionActivityResult(
                subscriptionId,
                interestId,
                interestName,
                interestKeywords,
                interestSubscriberCount,
                createdAt);
    }
}
