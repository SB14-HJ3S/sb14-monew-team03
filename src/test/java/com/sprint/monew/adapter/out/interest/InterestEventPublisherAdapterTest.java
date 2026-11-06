package com.sprint.monew.adapter.out.interest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sprint.monew.application.interest.event.InterestDeletedEvent;
import com.sprint.monew.application.interest.event.InterestKeywordsUpdatedEvent;
import com.sprint.monew.application.interest.event.InterestSubscribedEvent;
import com.sprint.monew.application.interest.event.InterestUnsubscribedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InterestEventPublisherAdapterTest {

    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private InterestEventPublisherAdapter adapter;

    @Test
    @DisplayName("관심사 구독 이벤트를 그대로 발행한다")
    void givenSubscribedEvent_whenPublish_thenForwardEvent() {
        InterestSubscribedEvent event =
                new InterestSubscribedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "경제",
                        List.of("금융"),
                        1L,
                        Instant.parse("2026-11-01T00:00:00Z"));

        adapter.publish(event);

        verify(eventPublisher).publishEvent(event);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    @DisplayName("관심사 구독 취소 이벤트를 그대로 발행한다")
    void givenUnsubscribedEvent_whenPublish_thenForwardEvent() {
        InterestUnsubscribedEvent event =
                new InterestUnsubscribedEvent(UUID.randomUUID(), UUID.randomUUID());

        adapter.publish(event);

        verify(eventPublisher).publishEvent(event);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    @DisplayName("관심사 키워드 수정 이벤트를 그대로 발행한다")
    void givenKeywordsUpdatedEvent_whenPublish_thenForwardEvent() {
        InterestKeywordsUpdatedEvent event =
                new InterestKeywordsUpdatedEvent(UUID.randomUUID(), List.of("금융", "주식"));

        adapter.publish(event);

        verify(eventPublisher).publishEvent(event);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    @DisplayName("관심사 삭제 이벤트를 그대로 발행한다")
    void givenDeletedEvent_whenPublish_thenForwardEvent() {
        InterestDeletedEvent event = new InterestDeletedEvent(UUID.randomUUID());

        adapter.publish(event);

        verify(eventPublisher).publishEvent(event);
        verifyNoMoreInteractions(eventPublisher);
    }
}
