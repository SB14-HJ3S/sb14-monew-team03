package com.sprint.monew.application.interest.event;

import java.util.UUID;

public record InterestUnsubscribedEvent(UUID userId, UUID interestId) {}
