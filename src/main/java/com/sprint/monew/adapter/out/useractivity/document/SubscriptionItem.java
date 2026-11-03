package com.sprint.monew.adapter.out.useractivity.document;

import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionItem(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        Instant createdAt) {
    public SubscriptionActivityResult toResult() {
        return new SubscriptionActivityResult(
                id, interestId, interestName, interestKeywords, interestSubscriberCount, createdAt);
    }
}
