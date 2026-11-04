package com.sprint.monew.application.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.ArticleInterestService;
import com.sprint.monew.application.interest.provided.dto.SubscriptionResult;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import java.time.Instant;
import java.util.List;
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

    @InjectMocks private InterestApplication interestApplication;

    @AfterEach
    void tearDown() {
        RequestIdContext.clear();
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
        InOrder inOrder = inOrder(userService, interestService, subscriptionService);
        inOrder.verify(userService).getActiveUser(requestUserId);
        inOrder.verify(interestService).getById(interestId);
        inOrder.verify(subscriptionService).create(captor.capture());
        inOrder.verify(interestService).increaseSubscriberCount(interestId);
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
        InOrder inOrder = inOrder(userService, interestService, subscriptionService);
        inOrder.verify(userService).getActiveUser(requestUserId);
        inOrder.verify(interestService).getById(interestId);
        inOrder.verify(subscriptionService).deleteByUserIdAndInterestId(requestUserId, interestId);
        inOrder.verify(interestService).decreaseSubscriberCount(interestId);
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
        InOrder inOrder = inOrder(interestService, articleInterestService, subscriptionService);
        inOrder.verify(interestService).getById(interestId);
        inOrder.verify(articleInterestService).deleteAllByInterestId(interestId);
        inOrder.verify(subscriptionService).deleteAllByInterestId(interestId);
        inOrder.verify(interestService).delete(deleting);
        inOrder.verifyNoMoreInteractions();
    }
}
