package com.sprint.monew.application.interest;

import com.sprint.monew.application.interest.provided.InterestUseCase;
import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.provided.dto.InterestResult;
import com.sprint.monew.application.user.ISubscriptionService;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.pagination.PageResult;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class InterestApplication implements InterestUseCase {

    private final InterestService interestService;
    private final ISubscriptionService subscriptionService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<InterestResult> findByQuery(InterestQuery query) {
        // 1. 유저 아이디에 해당하는 subscription 불러오기
        // 2. interest가 subscribed인지 검사
        // 3. dto로 반환
        PageResult<Interest> page = interestService.findPageByQuery(query);
        List<UUID> interestIds = page.content().stream().map(Interest::getId).toList();
        UUID requestId = RequestIdContext.get();
        Set<UUID> subscribedInterestIds =
                subscriptionService.findSubscribedInterestIds(requestId, interestIds);
        return page.map(
                interest ->
                        InterestResult.of(
                                interest, subscribedInterestIds.contains(interest.getId())));
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
