package com.sprint.monew.application.useractivity;

import com.sprint.monew.application.useractivity.provided.UserActivityUseCase;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.application.useractivity.required.UserActivityReader;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class UserActivityApplication implements UserActivityUseCase {
    private final UserActivityReader userActivityReader;

    @Override
    @Transactional(readOnly = true)
    public UserActivityResult findByUserId(UUID userId) {
        return userActivityReader
                .findByUserId(userId)
                .orElseThrow(
                        () ->
                                new MonewException(
                                        UserErrorType.USER_NOT_FOUND, Map.of("userId", userId)));
    }
}
