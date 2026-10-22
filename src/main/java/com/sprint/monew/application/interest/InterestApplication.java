package com.sprint.monew.application.interest;

import com.sprint.monew.application.interest.provided.InterestUseCase;
import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.provided.dto.InterestResult;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.pagination.PageResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class InterestApplication implements InterestUseCase {

    private final InterestService interestService;

    @Override
    public PageResult<InterestResult> findByQuery(InterestQuery query) {
        return null;
    }

    @Override
    @Transactional
    public InterestResult register(String name, List<String> keywords) {
        Interest created = interestService.create(name, keywords);
        return InterestResult.of(created, false);
    }

    @Override
    public InterestResult subscribe(UUID interestId, UUID requestId) {
        return null;
    }

    @Override
    public void unsubscribe(UUID interestId, UUID requestId) {}

    @Override
    public void delete(UUID interestId) {}

    @Override
    public InterestResult update(UUID interestId, List<String> keywords) {
        return null;
    }
}
