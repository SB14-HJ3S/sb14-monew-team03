package com.sprint.monew.adapter.in.batch.user.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.user.provided.usecase.UserUseCase;
import com.sprint.monew.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
class UserCleanupJobIntegrationTest {

    @MockitoBean private UserUseCase userUseCase;

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobExplorer jobExplorer;
    @Autowired private MeterRegistry meterRegistry;

    @Autowired
    @Qualifier("userCleanupJob")
    private Job userCleanupJob;

    @ParameterizedTest
    @ValueSource(ints = {0, 3})
    @DisplayName("사용자 삭제 성공 건수를 writeCount에 저장하고 메트릭에 한 번 반영한다")
    void recordDeletedCountWhenCleanupCompletes(int deletedCount) throws Exception {
        when(userUseCase.cleanupDeletedUsers()).thenReturn(deletedCount);
        double previousDeleted = deletedCounter();

        JobExecution execution = jobLauncher.run(userCleanupJob, parameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(deletedCounter()).isEqualTo(previousDeleted + deletedCount);
        assertPersistedStep(execution, BatchStatus.COMPLETED, deletedCount);
        verify(userUseCase).cleanupDeletedUsers();
    }

    @ParameterizedTest
    @MethodSource("cleanupFailures")
    @DisplayName("DB 오류나 예상하지 못한 오류는 FAILED로 저장하고 삭제 메트릭을 늘리지 않는다")
    void leaveDeletedCountUnchangedWhenCleanupFails(RuntimeException failure) throws Exception {
        when(userUseCase.cleanupDeletedUsers()).thenThrow(failure);
        double previousDeleted = deletedCounter();

        JobExecution execution = jobLauncher.run(userCleanupJob, parameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("FAILED");
        assertThat(execution.getAllFailureExceptions()).contains(failure);
        assertThat(deletedCounter()).isEqualTo(previousDeleted);
        assertPersistedStep(execution, BatchStatus.FAILED, 0);
    }

    @Test
    @DisplayName("실패한 Job을 같은 파라미터로 재시작하면 성공한 실행의 삭제 건수만 집계한다")
    void countOnlySuccessfulExecutionWhenFailedJobRestarts() throws Exception {
        when(userUseCase.cleanupDeletedUsers())
                .thenThrow(new DataAccessResourceFailureException("DB 오류"))
                .thenReturn(2);
        JobParameters parameters = parameters();
        double previousDeleted = deletedCounter();

        JobExecution failed = jobLauncher.run(userCleanupJob, parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(deletedCounter()).isEqualTo(previousDeleted);

        JobExecution restarted = jobLauncher.run(userCleanupJob, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getJobInstance().getId()).isEqualTo(failed.getJobInstance().getId());
        assertThat(restarted.getId()).isNotEqualTo(failed.getId());
        assertThat(deletedCounter()).isEqualTo(previousDeleted + 2);
        assertPersistedStep(failed, BatchStatus.FAILED, 0);
        assertPersistedStep(restarted, BatchStatus.COMPLETED, 2);
        verify(userUseCase, times(2)).cleanupDeletedUsers();
    }

    private void assertPersistedStep(
            JobExecution execution, BatchStatus expectedStatus, long expectedWriteCount) {
        JobExecution persisted = jobExplorer.getJobExecution(execution.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(expectedStatus);
        assertThat(persisted.getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getStepName()).isEqualTo("userCleanupStep");
                            assertThat(step.getStatus()).isEqualTo(expectedStatus);
                            assertThat(step.getWriteCount()).isEqualTo(expectedWriteCount);
                        });
    }

    private double deletedCounter() {
        return meterRegistry.get(UserCleanupMetrics.DELETED).counter().count();
    }

    private JobParameters parameters() {
        return new JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters();
    }

    private static Stream<RuntimeException> cleanupFailures() {
        return Stream.of(
                new DataAccessResourceFailureException("DB 오류"),
                new IllegalStateException("예상하지 못한 오류"));
    }
}
