package com.sprint.monew.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sprint.monew.domain.interest.entity.Interest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

    @Test
    @DisplayName("구독에서 사용자와 관심사의 ID를 조회한다.")
    void givenSubscription_whenGetRelatedIds_thenReturnUserAndInterestIds() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        User user = mock(User.class);
        Interest interest = mock(Interest.class);
        when(user.getId()).thenReturn(userId);
        when(interest.getId()).thenReturn(interestId);
        Subscription subscription = Subscription.create(user, interest);

        // when & then
        assertThat(subscription.getUserId()).isEqualTo(userId);
        assertThat(subscription.getInterestId()).isEqualTo(interestId);
    }
}
