package com.sprint.monew.application.interest;

import com.sprint.monew.application.interest.required.SubscriptionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements ISubscriptionService {

    private final SubscriptionRepository repository;

    @Override
    public Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds) {
        return repository.findSubscribedInterestIds(requestId, interestIds);
    }

    @Override
    public List<UUID> findSubscriberIds(UUID interestId) {
        return repository.findSubscribersByInterestId(interestId);
    }
}
