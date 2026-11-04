package com.sprint.monew.application.interest.event;

import java.util.List;
import java.util.UUID;

public record InterestKeywordsUpdatedEvent(UUID interestId, List<String> keywords) {}
