package com.sprint.monew.application.useractivity.provided.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionActivityResult(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        Instant createdAt) {}
