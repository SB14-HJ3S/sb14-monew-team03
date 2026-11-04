package com.sprint.monew.application.interest.required;

import com.sprint.monew.application.interest.event.InterestDeletedEvent;
import com.sprint.monew.application.interest.event.InterestKeywordsUpdatedEvent;
import com.sprint.monew.application.interest.event.InterestSubscribedEvent;
import com.sprint.monew.application.interest.event.InterestUnsubscribedEvent;

/** 관심사 이벤트 발행 포트 (활동 내역 갱신용) */
public interface InterestEventPublisherPort {
    void publish(InterestSubscribedEvent event);

    void publish(InterestUnsubscribedEvent event);

    void publish(InterestKeywordsUpdatedEvent event);

    void publish(InterestDeletedEvent event);
}
