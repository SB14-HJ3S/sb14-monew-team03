package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.application.interest.required.SubscriptionRepository;
import com.sprint.monew.domain.user.entity.Subscription;
import java.util.List;
import java.util.Optional;
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

    @Override
    public List<UUID> findSubscribersByInterestId(UUID interestId) {
        return repository.findSubscribersByInterestId(interestId);
    }

    @Override
    public void deleteAllByInterestId(UUID interestId) {
        repository.deleteAllByInterestId(interestId);
    }

    @Override
    public Subscription save(Subscription creating) {
        return repository.save(creating);
    }

    @Override
    public Optional<Subscription> findByUserIdAndInterestId(UUID userId, UUID interestId) {
        return repository.findByUserIdAndInterestId(userId, interestId);
    }

    @Override
    public void delete(Subscription deleting) {
        repository.delete(deleting);
    }

    @Override
    public List<UUID> findInterestIdsByUserId(UUID userId) {
        return repository.findInterestByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndInterestId(UUID userId, UUID interestId) {
        return repository.existsByUserIdAndInterestId(userId, interestId);
    }
}
