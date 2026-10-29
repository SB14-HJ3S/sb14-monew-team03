package com.sprint.monew.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.notification.provided.NotificationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupRunnerTest {

    @Mock private NotificationUseCase notificationUseCase;

    @InjectMocks private NotificationCleanupRunner runner;

    @Test
    @DisplayName("알림 삭제 배치를 실행하면 유스케이스를 호출한다.")
    void whenRunCleanup_thenCallsUseCase() {
        runner.runCleanup();

        verify(notificationUseCase).deleteOldReadNotifications();
    }

    @Test
    @DisplayName("삭제 중 DB 오류가 나도 예외를 밖으로 던지지 않는다.")
    void givenDbError_whenRunCleanup_thenDoesNotThrow() {
        doThrow(new DataAccessResourceFailureException("DB 오류"))
                .when(notificationUseCase)
                .deleteOldReadNotifications();

        assertThatCode(() -> runner.runCleanup()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("예상하지 못한 오류가 나도 예외를 밖으로 던지지 않는다.")
    void givenUnexpectedError_whenRunCleanup_thenDoesNotThrow() {
        doThrow(new IllegalStateException("예상 못한 오류"))
                .when(notificationUseCase)
                .deleteOldReadNotifications();

        assertThatCode(() -> runner.runCleanup()).doesNotThrowAnyException();
    }
}
