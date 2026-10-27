package com.sprint.monew.application.interest;

import com.sprint.monew.domain.user.entity.Subscription;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ISubscriptionService {
    Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds);

    List<UUID> findSubscriberIds(UUID interestId);

    void deleteAllByInterestId(UUID interestId);

    Subscription create(Subscription creating);

    void deleteByUserIdAndInterestId(UUID userId, UUID interestId);

    List<UUID> findInterestIdsByUserId(UUID id);
}
