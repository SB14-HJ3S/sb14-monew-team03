package com.sprint.monew.adapter.in.event.interest;

import com.sprint.monew.application.interest.provided.UserDeletionSubscriptionCleaner;
import com.sprint.monew.application.user.event.UserHardDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterestEventListener {

    private final UserDeletionSubscriptionCleaner cleaner;

    @EventListener
    public void onUserDeleted(UserHardDeletingEvent event) {
        cleaner.cleanupSubscriberCountBeforeUserHardDeletion(event.userId());
    }
}
