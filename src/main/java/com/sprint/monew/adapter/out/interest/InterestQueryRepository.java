package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.UUID;

public interface InterestQueryRepository {
    List<String> findAllNames();

    List<Interest> findByQuery(InterestQuery query);

    long countByQuery(InterestQuery query);

    Interest findByIdWithKeywords(UUID interestId);
}
