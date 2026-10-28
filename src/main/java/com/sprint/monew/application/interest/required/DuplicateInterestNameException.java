package com.sprint.monew.application.interest.required;

import java.util.UUID;
import lombok.Getter;

@Getter
public class DuplicateInterestNameException extends RuntimeException {

    private final UUID interestId;
    private final String interestName;

    public DuplicateInterestNameException(Throwable cause, UUID interestId, String interestName) {
        super(cause);
        this.interestId = interestId;
        this.interestName = interestName;
    }
}
