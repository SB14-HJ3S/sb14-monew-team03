package com.sprint.monew.application.interest.provided.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionResult(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        Long subscriberCount,
        Instant createdAt) {}
