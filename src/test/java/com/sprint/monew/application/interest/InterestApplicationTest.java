package com.sprint.monew.application.interest;

import static org.mockito.Mockito.inOrder;

import com.sprint.monew.application.article.IArticleInterestService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestApplicationTest {

    @Mock private InterestService interestService;
    @Mock private ISubscriptionService subscriptionService;
    @Mock private IArticleInterestService articleInterestService;

    @InjectMocks private InterestApplication interestApplication;

    @Test
    @DisplayName("관심사와 연관된 기사 및 구독 정보를 먼저 삭제한 뒤 관심사를 삭제한다.")
    void givenInterestId_whenDelete_thenDeleteRelationsBeforeInterest() {
        // given
        UUID interestId = UUID.randomUUID();

        // when
        interestApplication.delete(interestId);

        // then
        InOrder inOrder = inOrder(articleInterestService, subscriptionService, interestService);
        inOrder.verify(articleInterestService).deleteAllByInterestId(interestId);
        inOrder.verify(subscriptionService).deleteAllByInterestId(interestId);
        inOrder.verify(interestService).deleteById(interestId);
        inOrder.verifyNoMoreInteractions();
    }
}
