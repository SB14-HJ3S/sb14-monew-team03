package com.sprint.monew.application.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.ArticleInterestService;
import com.sprint.monew.application.interest.event.InterestDeletedEvent;
import com.sprint.monew.application.interest.event.InterestKeywordsUpdatedEvent;
import com.sprint.monew.application.interest.event.InterestSubscribedEvent;
import com.sprint.monew.application.interest.event.InterestUnsubscribedEvent;
import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.provided.dto.InterestResult;
import com.sprint.monew.application.interest.provided.dto.SubscriptionResult;
import com.sprint.monew.application.interest.required.InterestEventPublisherPort;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import com.sprint.monew.global.exception.errortype.SubscriptionErrorType;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestApplicationTest {

    @Mock private InterestService interestService;
    @Mock private ISubscriptionService subscriptionService;
    @Mock private ArticleInterestService articleInterestService;
    @Mock private IUserService userService;
    @Mock private InterestEventPublisherPort interestEventPublisher;

    @InjectMocks private InterestApplication interestApplication;

    @AfterEach
    void tearDown() {
        RequestIdContext.clear();
    }

    @Test
    @DisplayName("관심사 목록을 조회하면 요청 사용자의 구독 여부를 반영하고 페이지 정보를 유지한다.")
    void givenInterestsWithSubscriptions_whenFindByQuery_thenMapResultsAndPreservePageMetadata() {
        // given
        UUID requestUserId = UUID.randomUUID();
        UUID subscribedId = UUID.randomUUID();
        UUID unsubscribedId = UUID.randomUUID();
        Interest subscribed = mockInterest(subscribedId, "경제", List.of("금융"), 3L);
        Interest unsubscribed = mockInterest(unsubscribedId, "스포츠", List.of("축구"), 2L);
        InterestQuery query =
                new InterestQuery(
                        null, InterestQuery.SortOrder.NAME, SortDirection.ASC, null, null, 2);
        String nextCursor = unsubscribedId.toString();
        Instant nextAfter = Instant.parse("2026-11-01T00:00:00Z");
        PageResult<Interest> page =
                new PageResult<>(
                        List.of(subscribed, unsubscribed), nextCursor, nextAfter, 2, 5L, true);

        RequestIdContext.set(requestUserId);
        when(interestService.findPageByQuery(query)).thenReturn(page);
        when(subscriptionService.findSubscribedInterestIds(
                        requestUserId, List.of(subscribedId, unsubscribedId)))
                .thenReturn(Set.of(subscribedId));

        // when
        PageResult<InterestResult> result = interestApplication.findByQuery(query);

        // then
        assertThat(result.content())
                .containsExactly(
                        new InterestResult(subscribedId, "경제", List.of("금융"), 3L, true),
                        new InterestResult(unsubscribedId, "스포츠", List.of("축구"), 2L, false));
        assertThat(result.nextCursor()).isEqualTo(nextCursor);
        assertThat(result.nextAfter()).isEqualTo(nextAfter);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5L);
        assertThat(result.hasNext()).isTrue();
        verify(interestService).findPageByQuery(query);
        verify(subscriptionService)
                .findSubscribedInterestIds(requestUserId, List.of(subscribedId, unsubscribedId));
    }

    @Test
    @DisplayName("조회된 관심사가 없으면 빈 목록과 마지막 페이지 정보를 반환한다.")
    void givenEmptyPage_whenFindByQuery_thenReturnEmptyResults() {
        // given
        UUID requestUserId = UUID.randomUUID();
        InterestQuery query =
                new InterestQuery(
                        "없는 관심사", InterestQuery.SortOrder.NAME, SortDirection.ASC, null, null, 10);
        RequestIdContext.set(requestUserId);
        when(interestService.findPageByQuery(query))
                .thenReturn(new PageResult<>(List.of(), null, null, 0, 0L, false));
        when(subscriptionService.findSubscribedInterestIds(requestUserId, List.of()))
                .thenReturn(Set.of());

        // when
        PageResult<InterestResult> result = interestApplication.findByQuery(query);

        // then
        assertThat(result).isEqualTo(new PageResult<>(List.of(), null, null, 0, 0L, false));
        verify(subscriptionService).findSubscribedInterestIds(requestUserId, List.of());
    }

    @Test
    @DisplayName("관심사를 등록하면 생성된 관심사 정보와 구독 여부 false를 반환한다.")
    void givenNameAndKeywords_whenRegister_thenReturnCreatedInterestAsUnsubscribed() {
        // given
        UUID interestId = UUID.randomUUID();
        List<String> keywords = List.of("금융", "주식");
        Interest created = mockInterest(interestId, "경제", keywords, 0L);
        when(interestService.create("경제", keywords)).thenReturn(created);

        // when
        InterestResult result = interestApplication.register("경제", keywords);

        // then
        assertThat(result).isEqualTo(new InterestResult(interestId, "경제", keywords, 0L, false));
        verify(interestService).create("경제", keywords);
    }

    // 과제 결함인듯. 일단 테스트까지 구독여부 Null로 진행
    @Test
    @DisplayName("키워드를 수정하면 수정된 키워드로 이벤트를 발행하고 구독 여부 null을 반환한다.")
    void givenNewKeywords_whenUpdate_thenPublishUpdatedKeywordsAndReturnInterest() {
        // given
        UUID interestId = UUID.randomUUID();
        List<String> requestedKeywords = List.of("추가", "유지");
        List<String> updatedKeywords = List.of("유지", "추가");
        Interest updated = mockInterest(interestId, "관심사", updatedKeywords, 3L);
        when(interestService.update(interestId, requestedKeywords)).thenReturn(updated);

        // when
        InterestResult result = interestApplication.update(interestId, requestedKeywords);

        // then
        assertThat(result)
                .isEqualTo(new InterestResult(interestId, "관심사", updatedKeywords, 3L, null));
        InOrder inOrder = inOrder(interestService, interestEventPublisher);
        inOrder.verify(interestService).update(interestId, requestedKeywords);
        inOrder.verify(interestEventPublisher)
                .publish(new InterestKeywordsUpdatedEvent(interestId, updatedKeywords));
    }

    @Test
    @DisplayName("키워드 수정에 실패하면 예외를 전달하고 이벤트를 발행하지 않는다.")
    void givenNonExistingInterest_whenUpdate_thenPropagateExceptionWithoutPublishing() {
        // given
        UUID interestId = UUID.randomUUID();
        List<String> keywords = List.of("새 키워드");
        MonewException exception = new MonewException(InterestErrorType.INTEREST_NOT_FOUND);
        when(interestService.update(interestId, keywords)).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> interestApplication.update(interestId, keywords))
                .isSameAs(exception);
        verifyNoInteractions(interestEventPublisher);
    }

    @Test
    @DisplayName("요청 사용자가 관심사를 구독하면 구독을 저장하고 구독자 수를 증가시킨다.")
    void givenInterestAndRequestUser_whenSubscribe_thenCreateSubscriptionAndIncreaseCount() {
        // given
        UUID requestUserId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        List<String> keywords = List.of("키워드1", "키워드2");
        User user = mock(User.class);
        Interest interest = mock(Interest.class);
        Interest updatedInterest = mock(Interest.class);
        Subscription created = mock(Subscription.class);

        RequestIdContext.set(requestUserId);
        when(userService.getActiveUser(requestUserId)).thenReturn(user);
        when(interestService.getById(interestId)).thenReturn(interest);
        when(subscriptionService.create(any(Subscription.class))).thenReturn(created);
        when(interestService.increaseSubscriberCount(interestId)).thenReturn(updatedInterest);
        when(created.getId()).thenReturn(subscriptionId);
        when(created.getCreatedAt()).thenReturn(createdAt);
        when(updatedInterest.getId()).thenReturn(interestId);
        when(updatedInterest.getName()).thenReturn("관심사");
        when(updatedInterest.getKeywordNames()).thenReturn(keywords);
        when(updatedInterest.getSubscriberCount()).thenReturn(1L);

        // when
        SubscriptionResult result = interestApplication.subscribe(interestId);

        // then
        assertThat(result.id()).isEqualTo(subscriptionId);
        assertThat(result.interestId()).isEqualTo(interestId);
        assertThat(result.interestName()).isEqualTo("관심사");
        assertThat(result.interestKeywords()).containsExactlyElementsOf(keywords);
        assertThat(result.subscriberCount()).isEqualTo(1L);
        assertThat(result.createdAt()).isEqualTo(createdAt);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        InOrder inOrder =
                inOrder(userService, interestService, subscriptionService, interestEventPublisher);
        inOrder.verify(userService).getActiveUser(requestUserId);
        inOrder.verify(interestService).getById(interestId);
        inOrder.verify(subscriptionService).create(captor.capture());
        inOrder.verify(interestService).increaseSubscriberCount(interestId);
        inOrder.verify(interestEventPublisher)
                .publish(InterestSubscribedEvent.of(requestUserId, result));
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getInterest()).isSameAs(interest);
    }

    @Test
    @DisplayName("요청 사용자가 구독을 취소하면 구독을 삭제한 뒤 구독자 수를 감소시킨다.")
    void givenSubscription_whenUnsubscribe_thenDeleteSubscriptionAndDecreaseCount() {
        // given
        UUID requestUserId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        User user = mock(User.class);
        Interest interest = mock(Interest.class);

        RequestIdContext.set(requestUserId);
        when(userService.getActiveUser(requestUserId)).thenReturn(user);
        when(user.getId()).thenReturn(requestUserId);
        when(interestService.getById(interestId)).thenReturn(interest);
        when(interest.getId()).thenReturn(interestId);

        // when
        interestApplication.unsubscribe(interestId);

        // then
        InOrder inOrder =
                inOrder(userService, interestService, subscriptionService, interestEventPublisher);
        inOrder.verify(userService).getActiveUser(requestUserId);
        inOrder.verify(interestService).getById(interestId);
        inOrder.verify(subscriptionService).deleteByUserIdAndInterestId(requestUserId, interestId);
        inOrder.verify(interestService).decreaseSubscriberCount(interestId);
        inOrder.verify(interestEventPublisher)
                .publish(new InterestUnsubscribedEvent(requestUserId, interestId));
    }

    @Test
    @DisplayName("이미 구독한 관심사이면 구독자 수를 증가시키거나 이벤트를 발행하지 않는다.")
    void givenDuplicateSubscription_whenSubscribe_thenStopWithoutIncreasingCountOrPublishing() {
        // given
        UUID requestUserId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        User user = mock(User.class);
        Interest interest = mock(Interest.class);
        MonewException exception =
                new MonewException(SubscriptionErrorType.SUBSCRIPTION_ALREADY_EXISTS);

        RequestIdContext.set(requestUserId);
        when(userService.getActiveUser(requestUserId)).thenReturn(user);
        when(interestService.getById(interestId)).thenReturn(interest);
        when(subscriptionService.create(any(Subscription.class))).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> interestApplication.subscribe(interestId)).isSameAs(exception);
        verify(interestService, never()).increaseSubscriberCount(any(UUID.class));
        verifyNoInteractions(interestEventPublisher);
    }

    @Test
    @DisplayName("구독 정보가 없으면 구독자 수를 감소시키거나 이벤트를 발행하지 않는다.")
    void givenNoSubscription_whenUnsubscribe_thenStopWithoutDecreasingCountOrPublishing() {
        // given
        UUID requestUserId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        User user = mock(User.class);
        Interest interest = mock(Interest.class);
        MonewException exception = new MonewException(SubscriptionErrorType.SUBSCRIPTION_NOT_FOUND);

        RequestIdContext.set(requestUserId);
        when(userService.getActiveUser(requestUserId)).thenReturn(user);
        when(user.getId()).thenReturn(requestUserId);
        when(interestService.getById(interestId)).thenReturn(interest);
        when(interest.getId()).thenReturn(interestId);
        doThrow(exception)
                .when(subscriptionService)
                .deleteByUserIdAndInterestId(requestUserId, interestId);

        // when & then
        assertThatThrownBy(() -> interestApplication.unsubscribe(interestId)).isSameAs(exception);
        verify(interestService, never()).decreaseSubscriberCount(any(UUID.class));
        verifyNoInteractions(interestEventPublisher);
    }

    @Test
    @DisplayName("삭제 대상 사용자 목록을 전달해 관심사별 구독자 수를 정리한다.")
    void givenDeactivatedUsers_whenCleanupBeforeHardDelete_thenDecreaseSubscriberCounts() {
        // given
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        // when
        interestApplication.cleanupSubscriberCountBeforeUserHardDeletion(userIds);

        // then
        verify(interestService).decreaseSubscriberCountsByUserIds(userIds);
        verifyNoInteractions(userService, subscriptionService);
    }

    @Test
    @DisplayName("빈 사용자 목록도 관심사 서비스에 전달한다.")
    void givenEmptyUserIds_whenCleanupBeforeHardDelete_thenDelegateToInterestService() {
        interestApplication.cleanupSubscriberCountBeforeUserHardDeletion(List.of());

        verify(interestService).decreaseSubscriberCountsByUserIds(List.of());
        verifyNoInteractions(userService, subscriptionService);
    }

    @Test
    @DisplayName("관심사와 연관된 기사 및 구독 정보를 먼저 삭제한 뒤 관심사를 삭제한다.")
    void givenInterestId_whenDelete_thenDeleteRelationsBeforeInterest() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest deleting = Interest.create("관심사", List.of("키워드"));
        when(interestService.getById(interestId)).thenReturn(deleting);

        // when
        interestApplication.delete(interestId);

        // then
        InOrder inOrder =
                inOrder(
                        interestService,
                        articleInterestService,
                        subscriptionService,
                        interestEventPublisher);
        inOrder.verify(interestService).getById(interestId);
        inOrder.verify(articleInterestService).deleteAllByInterestId(interestId);
        inOrder.verify(subscriptionService).deleteAllByInterestId(interestId);
        inOrder.verify(interestService).delete(deleting);
        inOrder.verify(interestEventPublisher).publish(new InterestDeletedEvent(interestId));
        inOrder.verifyNoMoreInteractions();
    }

    private Interest mockInterest(
            UUID id, String name, List<String> keywords, long subscriberCount) {
        Interest interest = mock(Interest.class);
        when(interest.getId()).thenReturn(id);
        when(interest.getName()).thenReturn(name);
        when(interest.getKeywordNames()).thenReturn(keywords);
        when(interest.getSubscriberCount()).thenReturn(subscriberCount);
        return interest;
    }
}
