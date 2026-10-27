package com.sprint.monew.application.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.IArticleInterestService;
import com.sprint.monew.application.interest.provided.dto.SubscriptionResult;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
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
    @Mock private IArticleInterestService articleInterestService;
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
    @DisplayName("비활성 사용자임을 검증한 뒤 구독한 관심사들의 구독자 수를 감소시킨다.")
    void givenDeactivatedUser_whenCleanupBeforeHardDelete_thenDecreaseSubscriberCounts() {
        // given
        UUID userId = UUID.randomUUID();
        List<UUID> interestIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        when(subscriptionService.findInterestIdsByUserId(userId)).thenReturn(interestIds);

        // when
        interestApplication.cleanupSubscriberCountBeforeUserHardDeletion(userId);

        // then
        InOrder inOrder = inOrder(userService, subscriptionService, interestService);
        inOrder.verify(userService).validateDeactivatedUser(userId);
        inOrder.verify(subscriptionService).findInterestIdsByUserId(userId);
        inOrder.verify(interestService).decreaseSubscriberCount(interestIds);
    }

    @Test
    @DisplayName("물리 삭제 대상 사용자가 아니면 구독자 수를 감소시키지 않는다.")
    void givenInvalidUser_whenCleanupBeforeHardDelete_thenDoNotDecreaseSubscriberCounts() {
        // given
        UUID userId = UUID.randomUUID();
        MonewException exception = new MonewException(UserErrorType.USER_NOT_FOUND);
        doThrow(exception).when(userService).validateDeactivatedUser(userId);

        // when & then
        assertThatThrownBy(
                        () ->
                                interestApplication.cleanupSubscriberCountBeforeUserHardDeletion(
                                        userId))
                .isSameAs(exception);
        verify(userService).validateDeactivatedUser(userId);
        verifyNoInteractions(subscriptionService, interestService);
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
