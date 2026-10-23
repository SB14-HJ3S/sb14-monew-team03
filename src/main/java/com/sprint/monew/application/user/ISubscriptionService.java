package com.sprint.monew.application.user;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ISubscriptionService {
    Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds);
}
