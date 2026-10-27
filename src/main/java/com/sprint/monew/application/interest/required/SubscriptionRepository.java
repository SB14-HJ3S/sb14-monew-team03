package com.sprint.monew.application.interest.required;

import com.sprint.monew.domain.user.entity.Subscription;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SubscriptionRepository {
    Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds);

    List<UUID> findSubscribersByInterestId(UUID interestId);

    void deleteAllByInterestId(UUID interestId);

    Subscription save(Subscription creating);
}
