package com.sprint.monew.adapter.in.batch.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.log.provided.LogBackupUseCase;
import com.sprint.monew.application.log.provided.dto.LogBackupResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

@ExtendWith(MockitoExtension.class)
class LogBackupMetricsTest {

    @Mock private LogBackupUseCase logBackupUseCase;

    @ParameterizedTest
    @CsvSource({"COMPLETED, 3, 3", "FAILED, 3, 2", "FAILED, 3, 0", "COMPLETED, 0, 0"})
    @DisplayName("Step 성공 여부와 관계없이 파일 수를 집계하고 기존 종료 상태를 유지한다")
    void publishBackupResultRegardlessOfStepStatus(
            BatchStatus status, int expectedTotal, int expectedUploaded) throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 10, 31);
        JobParameters parameters =
                new JobParametersBuilder()
                        .addLocalDate("logTargetDate", targetDate)
                        .toJobParameters();
        JobExecution jobExecution = new JobExecution(1L, parameters);
        StepExecution stepExecution = jobExecution.createStepExecution("logBackupStep");
        when(logBackupUseCase.backup(targetDate))
                .thenReturn(new LogBackupResult(expectedTotal, expectedUploaded));
        LogBackupTasklet tasklet = new LogBackupTasklet(logBackupUseCase);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            LogBackupStepListener listener = new LogBackupStepListener(registry);

            if (status == BatchStatus.FAILED) {
                assertThatThrownBy(
                                () ->
                                        tasklet.execute(
                                                new StepContribution(stepExecution),
                                                new ChunkContext(new StepContext(stepExecution))))
                        .isInstanceOf(LogBackupFailedException.class);
            } else {
                tasklet.execute(
                        new StepContribution(stepExecution),
                        new ChunkContext(new StepContext(stepExecution)));
            }
            ExitStatus exitStatus =
                    status == BatchStatus.FAILED ? ExitStatus.FAILED : ExitStatus.COMPLETED;
            stepExecution.setStatus(status);
            stepExecution.setExitStatus(exitStatus);

            assertThat(listener.afterStep(stepExecution)).isNull();
            assertThat(stepExecution.getStatus()).isEqualTo(status);
            assertThat(stepExecution.getExitStatus()).isEqualTo(exitStatus);

            assertThat(registry.get(LogMetrics.TOTAL_FILES).counter().count())
                    .isEqualTo(expectedTotal);
            assertThat(registry.get(LogMetrics.UPLOADED_FILES).counter().count())
                    .isEqualTo(expectedUploaded);
        } finally {
            registry.close();
        }
    }
}
