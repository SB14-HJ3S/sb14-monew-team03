package com.sprint.monew.adapter.in.batch.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.notification.provided.usecase.NotificationUseCase;
import com.sprint.monew.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

@IntegrationTest
class NotificationCleanupJobIntegrationTest {

    @MockitoBean private NotificationUseCase notificationUseCase;

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobExplorer jobExplorer;
    @Autowired private JobRepository jobRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MeterRegistry meterRegistry;

    @Autowired
    @Qualifier("notificationCleanupJob")
    private Job notificationCleanupJob;

    @Autowired
    @Qualifier("notificationCleanupStep")
    private Step notificationCleanupStep;

    @ParameterizedTest
    @ValueSource(ints = {0, 3})
    @DisplayName("삭제 성공 건수를 writeCount에 저장하고 메트릭에 한 번 반영한다")
    void recordDeletedCountWhenCleanupCompletes(int deletedCount) throws Exception {
        when(notificationUseCase.deleteOldReadNotifications()).thenReturn(deletedCount);
        double previousDeleted = deletedCounter();

        JobExecution execution = jobLauncher.run(notificationCleanupJob, parameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedCounter()).isEqualTo(previousDeleted + deletedCount);
        assertThat(jobExplorer.getJobExecution(execution.getId()).getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getStatus()).isEqualTo(BatchStatus.COMPLETED);
                            assertThat(step.getWriteCount()).isEqualTo(deletedCount);
                        });
    }

    @Test
    @DisplayName("삭제 실패 시 Job을 FAILED로 저장하고 삭제 메트릭을 늘리지 않는다")
    void leaveDeletedCountUnchangedWhenCleanupFails() throws Exception {
        when(notificationUseCase.deleteOldReadNotifications())
                .thenThrow(new DataAccessResourceFailureException("DB 오류"));
        double previousDeleted = deletedCounter();

        JobExecution execution = jobLauncher.run(notificationCleanupJob, parameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(deletedCounter()).isEqualTo(previousDeleted);
        assertThat(jobExplorer.getJobExecution(execution.getId()).getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getStatus()).isEqualTo(BatchStatus.FAILED);
                            assertThat(step.getWriteCount()).isZero();
                        });
        assertThat(execution.getAllFailureExceptions())
                .anySatisfy(
                        failure ->
                                assertThat(failure)
                                        .isInstanceOf(DataAccessResourceFailureException.class));
    }

    @Test
    @DisplayName("후속 Step이 실패해도 완료된 삭제 Step의 메트릭은 반영한다")
    void countCommittedCleanupEvenWhenFollowingStepFails() throws Exception {
        when(notificationUseCase.deleteOldReadNotifications()).thenReturn(3);
        double previousDeleted = deletedCounter();
        Step failingStep =
                new StepBuilder("afterNotificationCleanupFailureStep", jobRepository)
                        .tasklet(
                                (contribution, context) -> {
                                    throw new IllegalStateException("후속 Step 실패");
                                },
                                transactionManager)
                        .build();
        Job job =
                new JobBuilder("notificationCleanupFollowedByFailureJob", jobRepository)
                        .start(notificationCleanupStep)
                        .next(failingStep)
                        .build();

        JobExecution execution = jobLauncher.run(job, parameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(deletedCounter()).isEqualTo(previousDeleted + 3);
        assertThat(jobExplorer.getJobExecution(execution.getId()).getStepExecutions())
                .filteredOn(step -> step.getStepName().equals("notificationCleanupStep"))
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getStatus()).isEqualTo(BatchStatus.COMPLETED);
                            assertThat(step.getWriteCount()).isEqualTo(3);
                        });
    }

    private double deletedCounter() {
        return meterRegistry.get(NotificationMetrics.DELETED).counter().count();
    }

    private JobParameters parameters() {
        return new JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters();
    }
}
