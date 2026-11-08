package com.sprint.monew.application.interest;

import com.sprint.monew.application.article.provided.service.ArticleInterestService;
import com.sprint.monew.application.interest.event.InterestDeletedEvent;
import com.sprint.monew.application.interest.event.InterestKeywordsUpdatedEvent;
import com.sprint.monew.application.interest.event.InterestSubscribedEvent;
import com.sprint.monew.application.interest.event.InterestUnsubscribedEvent;
import com.sprint.monew.application.interest.provided.service.ISubscriptionService;
import com.sprint.monew.application.interest.provided.service.InterestService;
import com.sprint.monew.application.interest.provided.usecase.InterestUseCase;
import com.sprint.monew.application.interest.provided.usecase.UserDeletionSubscriptionCleaner;
import com.sprint.monew.application.interest.provided.usecase.dto.InterestQuery;
import com.sprint.monew.application.interest.provided.usecase.dto.InterestResult;
import com.sprint.monew.application.interest.provided.usecase.dto.SubscriptionResult;
import com.sprint.monew.application.interest.required.InterestEventPublisherPort;
import com.sprint.monew.application.user.provided.service.IUserService;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
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
public class InterestApplication implements InterestUseCase, UserDeletionSubscriptionCleaner {

    private final InterestService interestService;
    private final ISubscriptionService subscriptionService;
    private final ArticleInterestService articleInterestService;
    private final IUserService userService;
    private final InterestEventPublisherPort interestEventPublisher;

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
    @Transactional
    public SubscriptionResult subscribe(UUID interestId) {
        UUID userId = RequestIdContext.get();
        User user = userService.getActiveUser(userId);
        Interest interest = interestService.getById(interestId);

        Subscription creating = Subscription.create(user, interest);
        Subscription created = subscriptionService.create(creating);
        Interest updatedInterest = interestService.increaseSubscriberCount(interestId);

        SubscriptionResult result = SubscriptionResult.of(updatedInterest, created);
        interestEventPublisher.publish(InterestSubscribedEvent.of(userId, result));
        return result;
    }

    @Override
    @Transactional
    public void unsubscribe(UUID interestId) {
        UUID userId = RequestIdContext.get();
        User user = userService.getActiveUser(userId);
        Interest interest = interestService.getById(interestId);
        subscriptionService.deleteByUserIdAndInterestId(user.getId(), interest.getId());
        interestService.decreaseSubscriberCount(interestId);

        interestEventPublisher.publish(new InterestUnsubscribedEvent(userId, interestId));
    }

    @Override
    @Transactional
    public void delete(UUID interestId) {
        Interest deleting = interestService.getById(interestId);
        articleInterestService.deleteAllByInterestId(interestId);
        subscriptionService.deleteAllByInterestId(interestId);
        interestService.delete(deleting);

        interestEventPublisher.publish(new InterestDeletedEvent(interestId));
    }

    @Override
    @Transactional
    public InterestResult update(UUID interestId, List<String> keywords) {
        Interest updated = interestService.update(interestId, keywords);

        interestEventPublisher.publish(
                new InterestKeywordsUpdatedEvent(interestId, updated.getKeywordNames()));
        // 응답 필드에 subscribedByMe는 있는데 데모 페이지에 항상 null로 뜸
        // interest patch 요청할 때 프론트에서 request-user-id를 아예 안주므로 일단 맞춰서 null로 반환해놓음
        return InterestResult.of(updated, null);
    }

    @Override
    @Transactional
    public void cleanupSubscriberCountBeforeUserHardDeletion(List<UUID> userIds) {
        interestService.decreaseSubscriberCountsByUserIds(userIds);
    }
}
