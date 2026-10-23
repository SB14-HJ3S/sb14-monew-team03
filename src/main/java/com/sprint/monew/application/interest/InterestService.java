package com.sprint.monew.application.interest;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.pagination.PageResult;
import java.util.List;

public interface InterestService {
    Interest create(String name, List<String> keywords);

    PageResult<Interest> findPageByQuery(InterestQuery query);
}
