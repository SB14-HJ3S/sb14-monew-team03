package com.sprint.monew.application.interest.required;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterestRepository {
    Interest save(Interest entity);

    List<String> findAllNames();

    List<Interest> findByQuery(InterestQuery query);

    long countByQuery(InterestQuery query);

    Optional<Interest> findByIdWithKeywords(UUID interestId);

    void delete(Interest entity);

    Optional<Interest> findById(UUID id);

    int increaseSubscriberCount(UUID interestId);
}
