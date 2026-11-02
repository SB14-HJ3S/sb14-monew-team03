package com.sprint.monew.adapter.out.user;

import com.sprint.monew.application.user.event.UserHardDeletingEvent;
import com.sprint.monew.application.user.event.UserNicknameUpdatedEvent;
import com.sprint.monew.application.user.event.UserRegisteredEvent;
import com.sprint.monew.application.user.required.UserEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventPublisherAdapter implements UserEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(UserRegisteredEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(UserNicknameUpdatedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(UserHardDeletingEvent event) {
        eventPublisher.publishEvent(event);
    }
}
