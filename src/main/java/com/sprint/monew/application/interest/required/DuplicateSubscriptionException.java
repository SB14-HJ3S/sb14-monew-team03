package com.sprint.monew.application.interest.required;

import java.util.UUID;
import lombok.Getter;

@Getter
public class DuplicateSubscriptionException extends RuntimeException {

    private final UUID userId;
    private final UUID interestId;

    public DuplicateSubscriptionException(Throwable cause, UUID userId, UUID interestId) {
        super(cause);
        this.userId = userId;
        this.interestId = interestId;
    }
}
