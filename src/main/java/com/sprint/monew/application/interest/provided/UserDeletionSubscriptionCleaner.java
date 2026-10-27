package com.sprint.monew.application.interest.provided;

import java.util.UUID;

public interface UserDeletionSubscriptionCleaner {
    void cleanupSubscriberCountBeforeUserHardDeletion(UUID userId);
}
