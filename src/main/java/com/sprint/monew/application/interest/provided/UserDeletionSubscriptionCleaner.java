package com.sprint.monew.application.interest.provided;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public interface UserDeletionSubscriptionCleaner {
    void cleanupSubscriberCountBeforeUserHardDeletion(@NotEmpty List<@NotNull UUID> userIds);
}
