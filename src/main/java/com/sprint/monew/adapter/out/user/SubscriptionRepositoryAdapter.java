package com.sprint.monew.adapter.out.user;

import com.sprint.monew.application.user.required.SubscriptionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {
    private final SubscriptionJpaRepository repository;

    @Override
    public Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds) {
        return repository.findSubscribedInterestIds(requestId, interestIds);
    }
}
