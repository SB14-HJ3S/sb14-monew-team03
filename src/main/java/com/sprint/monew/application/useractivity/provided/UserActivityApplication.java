package com.sprint.monew.application.useractivity.provided;

import com.sprint.monew.application.notification.required.UserActivityReader;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
