package com.sprint.monew.application.interest.provided;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.UUID;

public interface InterestUseCase {
    Interest findByQuery(InterestQuery query);

    Interest register(String name, List<String> keywords);

    Interest subscribe(UUID interestId, UUID requestId);

    void unsubscribe(UUID interestId, UUID requestId);

    void delete(UUID interestId);

    Interest update(UUID interestId, List<String> keywords);
}
