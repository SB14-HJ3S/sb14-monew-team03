package com.sprint.monew.application.user.required;

import com.sprint.monew.application.user.event.UserRegisteredEvent;

public interface UserEventPublisherPort {

    void publish(UserRegisteredEvent event);
}
