package com.sprint.monew.application.interest;

import com.sprint.monew.application.article.IArticleInterestService;
import com.sprint.monew.application.interest.provided.InterestUseCase;
import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.provided.dto.InterestResult;
import com.sprint.monew.application.interest.provided.dto.SubscriptionResult;
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
    private final IArticleInterestService articleInterestService;

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
    public SubscriptionResult subscribe(UUID interestId) {
        return null;
    }

    @Override
    public void unsubscribe(UUID interestId) {}

    @Override
    @Transactional
    public void delete(UUID interestId) {
        Interest deleting = interestService.getById(interestId);
        articleInterestService.deleteAllByInterestId(interestId);
        subscriptionService.deleteAllByInterestId(interestId);
        interestService.delete(deleting);
    }

    @Override
    @Transactional
    public InterestResult update(UUID interestId, List<String> keywords) {
        Interest updated = interestService.update(interestId, keywords);
        // 응답 필드에 subscribedByMe는 있는데 데모 페이지에 항상 null로 뜸
        // interest patch 요청할 때 프론트에서 request-user-id를 아예 안주므로 일단 맞춰서 null로 반환해놓음
        return InterestResult.of(updated, null);
    }
}
