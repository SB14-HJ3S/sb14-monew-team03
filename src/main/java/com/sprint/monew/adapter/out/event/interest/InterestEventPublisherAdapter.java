package com.sprint.monew.adapter.out.event.interest;

import com.sprint.monew.application.interest.event.InterestDeletedEvent;
import com.sprint.monew.application.interest.event.InterestKeywordsUpdatedEvent;
import com.sprint.monew.application.interest.event.InterestSubscribedEvent;
import com.sprint.monew.application.interest.event.InterestUnsubscribedEvent;
import com.sprint.monew.application.interest.required.InterestEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterestEventPublisherAdapter implements InterestEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(InterestSubscribedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(InterestUnsubscribedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(InterestKeywordsUpdatedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(InterestDeletedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
