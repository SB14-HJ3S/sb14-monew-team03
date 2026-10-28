package com.sprint.monew.application.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.interest.required.SubscriptionRepository;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.SubscriptionErrorType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository repository;

    @Mock private Subscription subscription;

    @InjectMocks private SubscriptionService service;

    @Test
    @DisplayName("구독을 저장하고 저장된 구독을 반환한다.")
    void givenSubscription_whenCreate_thenSaveAndReturnSubscription() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        when(subscription.getUserId()).thenReturn(userId);
        when(subscription.getInterestId()).thenReturn(interestId);
        when(repository.existsByUserIdAndInterestId(userId, interestId)).thenReturn(false);
        when(repository.save(subscription)).thenReturn(subscription);

        // when
        Subscription created = service.create(subscription);

        // then
        assertThat(created).isSameAs(subscription);
        InOrder inOrder = inOrder(repository);
        inOrder.verify(repository).existsByUserIdAndInterestId(userId, interestId);
        inOrder.verify(repository).save(subscription);
    }

    @Test
    @DisplayName("동일한 사용자가 이미 구독한 관심사를 다시 구독하면 예외가 발생한다.")
    void givenExistingSubscription_whenCreate_thenThrowException() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        when(subscription.getUserId()).thenReturn(userId);
        when(subscription.getInterestId()).thenReturn(interestId);
        when(repository.existsByUserIdAndInterestId(userId, interestId)).thenReturn(true);

        // when
        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> service.create(subscription));

        // then
        assertThat(thrown.getErrorType())
                .isEqualTo(SubscriptionErrorType.SUBSCRIPTION_ALREADY_EXISTS);
        assertThat(thrown.getDetails())
                .containsEntry("userId", userId)
                .containsEntry("interestId", interestId);
        verify(repository).existsByUserIdAndInterestId(userId, interestId);
        verify(repository, never()).save(subscription);
    }

    @Test
    @DisplayName("사용자와 관심사에 해당하는 구독을 삭제한다.")
    void givenExistingSubscription_whenDelete_thenDeleteSubscription() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        when(repository.findByUserIdAndInterestId(userId, interestId))
                .thenReturn(Optional.of(subscription));

        // when
        service.deleteByUserIdAndInterestId(userId, interestId);

        // then
        verify(repository).findByUserIdAndInterestId(userId, interestId);
        verify(repository).delete(subscription);
    }

    @Test
    @DisplayName("사용자와 관심사에 해당하는 구독이 없으면 예외가 발생한다.")
    void givenNonExistingSubscription_whenDelete_thenThrowException() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        when(repository.findByUserIdAndInterestId(userId, interestId)).thenReturn(Optional.empty());

        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class,
                        () -> service.deleteByUserIdAndInterestId(userId, interestId));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(SubscriptionErrorType.SUBSCRIPTION_NOT_FOUND);
        assertThat(thrown.getDetails())
                .containsEntry("userId", userId)
                .containsEntry("interestId", interestId);
        verify(repository).findByUserIdAndInterestId(userId, interestId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("사용자가 구독한 관심사 ID 목록을 조회한다.")
    void givenUserId_whenFindInterestIds_thenReturnInterestIds() {
        // given
        UUID userId = UUID.randomUUID();
        List<UUID> interestIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findInterestIdsByUserId(userId)).thenReturn(interestIds);

        // when
        List<UUID> result = service.findInterestIdsByUserId(userId);

        // then
        assertThat(result).containsExactlyElementsOf(interestIds);
        verify(repository).findInterestIdsByUserId(userId);
    }
}
