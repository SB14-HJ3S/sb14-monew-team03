package com.sprint.monew.application.useractivity.required;

import com.sprint.monew.application.useractivity.provided.usecase.dto.UserActivityResult;
import java.util.Optional;
import java.util.UUID;

public interface UserActivitySourceReader {

    Optional<UserActivityResult> findByUserId(UUID userId);
}
