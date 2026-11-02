package com.sprint.monew.application.useractivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.application.useractivity.required.UserActivityReader;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityApplicationTest {

    @Mock private UserActivityReader userActivityReader;

    @InjectMocks private UserActivityApplication userActivityApplication;

    @Test
    @DisplayName("활동 내역이 있으면 그대로 반환한다.")
    void givenExistingUser_whenFind_thenReturnsActivity() {
        UUID userId = UUID.randomUUID();
        UserActivityResult activity =
                new UserActivityResult(
                        userId,
                        "woody@monew.com",
                        "우디",
                        Instant.now(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of());
        when(userActivityReader.findByUserId(userId)).thenReturn(Optional.of(activity));

        UserActivityResult result = userActivityApplication.findByUserId(userId);

        assertThat(result).isEqualTo(activity);
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자면 USER_NOT_FOUND 예외를 던진다.")
    void givenUnknownUser_whenFind_thenThrowsUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userActivityReader.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userActivityApplication.findByUserId(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
    }
}
