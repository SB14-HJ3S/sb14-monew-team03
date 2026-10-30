package com.sprint.monew.application.interest;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.pagination.PageResult;
import java.util.List;
import java.util.UUID;

public interface InterestService {
    Interest create(String name, List<String> keywords);

    PageResult<Interest> findPageByQuery(InterestQuery query);

    List<Interest> findSubscribedWithKeywords();

    Interest update(UUID interestId, List<String> keywords);

    Interest getById(UUID interestId);

    void delete(Interest deleting);

    Interest increaseSubscriberCount(UUID interestId);

    void decreaseSubscriberCount(UUID interestId);

    void decreaseSubscriberCount(List<UUID> interestIds);
}
