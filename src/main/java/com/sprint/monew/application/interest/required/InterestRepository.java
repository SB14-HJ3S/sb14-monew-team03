package com.sprint.monew.application.interest.required;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface InterestRepository {
    List<Interest> findAllById(Set<UUID> ids);

    Interest save(Interest entity);

    List<String> findAllNames();

    List<Interest> findByQuery(InterestQuery query);

    long countByQuery(InterestQuery query);

    Optional<Interest> findByIdWithKeywords(UUID interestId);

    void delete(Interest entity);

    Optional<Interest> findById(UUID id);

    void increaseSubscriberCount(UUID interestId);

    void decreaseSubscriberCount(UUID interestId);

    boolean existsById(UUID interestId);

    void decreaseSubscriberCountsByUserIds(List<UUID> userIds);

    List<Interest> findSubscribedWithKeywords();
}
