package com.sprint.monew.application.interest;

import com.sprint.monew.application.interest.required.SubscriptionRepository;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.SubscriptionErrorType;
import java.util.List;
import java.util.Map;
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

    @Override
    public void deleteAllByInterestId(UUID interestId) {
        repository.deleteAllByInterestId(interestId);
    }

    @Override
    public Subscription create(Subscription creating) {
        return repository.save(creating);
    }

    @Override
    public void deleteByUserIdAndInterestId(UUID userId, UUID interestId) {
        Subscription deleting =
                repository
                        .findByUserIdAndInterestId(userId, interestId)
                        .orElseThrow(
                                () ->
                                        new MonewException(
                                                SubscriptionErrorType.SUBSCRIPTION_NOT_FOUND,
                                                Map.of(
                                                        "userId",
                                                        userId,
                                                        "interestId",
                                                        interestId)));
        repository.delete(deleting);
    }

    @Override
    public List<UUID> findInterestIdsByUserId(UUID userId) {
        return repository.findInterestIdsByUserId(userId);
    }
}
