package com.sprint.monew.application.interest.required;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SubscriptionRepository {
    Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds);
}
