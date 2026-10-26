package com.sprint.monew.application.interest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ISubscriptionService {
    Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds);

    List<UUID> findSubscriberIds(UUID interestId);
}
