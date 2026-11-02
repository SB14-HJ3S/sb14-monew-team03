package com.sprint.monew.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.user.provided.UserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class UserPurgeRunnerTest {

    @Mock private UserUseCase userUseCase;

    @InjectMocks private UserPurgeRunner runner;

    @Test
    @DisplayName("사용자 완전 삭제 배치를 실행하면 유스케이스를 호출한다")
    void whenRunPurge_thenCallsUseCase() {
        runner.runPurge();

        verify(userUseCase).purgeDeletedUsers();
    }

    @Test
    @DisplayName("삭제 중 DB 오류가 나도 예외를 밖으로 던지지 않는다")
    void givenDbError_whenRunPurge_thenDoesNotThrow() {
        doThrow(new DataAccessResourceFailureException("DB 오류"))
                .when(userUseCase)
                .purgeDeletedUsers();

        assertThatCode(() -> runner.runPurge()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("예상하지 못한 오류가 나도 예외를 밖으로 던지지 않는다")
    void givenUnexpectedError_whenRunPurge_thenDoesNotThrow() {
        doThrow(new IllegalStateException("예상 못한 오류")).when(userUseCase).purgeDeletedUsers();

        assertThatCode(() -> runner.runPurge()).doesNotThrowAnyException();
    }
}
