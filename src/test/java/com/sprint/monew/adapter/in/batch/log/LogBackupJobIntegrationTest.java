package com.sprint.monew.adapter.in.batch.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.log.provided.LogBackupUseCase;
import com.sprint.monew.application.log.provided.dto.LogBackupResult;
import com.sprint.monew.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
class LogBackupJobIntegrationTest {

    @MockitoBean private LogBackupUseCase logBackupUseCase;

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobExplorer jobExplorer;
    @Autowired private MeterRegistry meterRegistry;

    @Autowired
    @Qualifier("logBackupJob")
    private Job logBackupJob;

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    @DisplayName("일부 또는 전체 업로드 실패를 FAILED로 저장하고 부분 성공 수를 메트릭에 반영한다")
    void failBackupAndCountPartialUploads(int uploadedFileCount) throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 10, 1).plusDays(uploadedFileCount);
        JobParameters parameters = parameters(targetDate);
        when(logBackupUseCase.backup(targetDate))
                .thenReturn(new LogBackupResult(3, uploadedFileCount));
        double previousTotal = meterRegistry.get(LogMetrics.TOTAL_FILES).counter().count();
        double previousUploaded = meterRegistry.get(LogMetrics.UPLOADED_FILES).counter().count();

        JobExecution failed = jobLauncher.run(logBackupJob, parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failed.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
        assertMetricCounts(previousTotal + 3, previousUploaded + uploadedFileCount);
        assertThat(jobExplorer.getJobExecution(failed.getId()).getStatus())
                .isEqualTo(BatchStatus.FAILED);
        assertThat(failed.getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getStatus()).isEqualTo(BatchStatus.FAILED);
                            assertThat(step.getWriteCount()).isEqualTo(uploadedFileCount);
                        });
        assertThat(failed.getAllFailureExceptions())
                .anySatisfy(
                        failure ->
                                assertThat(failure)
                                        .isInstanceOf(LogBackupFailedException.class)
                                        .hasMessageContaining("targetDate=" + targetDate)
                                        .hasMessageContaining("totalFiles=3")
                                        .hasMessageContaining(
                                                "uploadedFiles=" + uploadedFileCount));

        verify(logBackupUseCase).backup(targetDate);
    }

    @Test
    @DisplayName("결과 집계 전에 실패하면 배치는 FAILED가 되고 파일 수 카운터는 증가하지 않는다")
    void leaveCountersUnchangedWhenBackupFailsBeforeProducingResult() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 10, 20);
        JobParameters parameters = parameters(targetDate);
        when(logBackupUseCase.backup(targetDate)).thenThrow(new IOException("로그 디렉터리 조회 실패"));
        double previousTotal = meterRegistry.get(LogMetrics.TOTAL_FILES).counter().count();
        double previousUploaded = meterRegistry.get(LogMetrics.UPLOADED_FILES).counter().count();

        JobExecution execution = jobLauncher.run(logBackupJob, parameters);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getExitStatus().getExitCode())
                .isEqualTo(ExitStatus.FAILED.getExitCode());
        assertThat(execution.getAllFailureExceptions())
                .anySatisfy(failure -> assertThat(failure).isInstanceOf(IOException.class));
        assertMetricCounts(previousTotal, previousUploaded);
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "3, 3"})
    @DisplayName("대상 파일이 없거나 모든 파일의 업로드가 성공하면 COMPLETED로 저장한다")
    void completeBackupWhenNoUploadsFail(int totalFileCount, int uploadedFileCount)
            throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 10, 10).plusDays(totalFileCount);
        when(logBackupUseCase.backup(targetDate))
                .thenReturn(new LogBackupResult(totalFileCount, uploadedFileCount));
        double previousTotal = meterRegistry.get(LogMetrics.TOTAL_FILES).counter().count();
        double previousUploaded = meterRegistry.get(LogMetrics.UPLOADED_FILES).counter().count();

        JobExecution execution = jobLauncher.run(logBackupJob, parameters(targetDate));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus().getExitCode())
                .isEqualTo(ExitStatus.COMPLETED.getExitCode());
        assertMetricCounts(previousTotal + totalFileCount, previousUploaded + uploadedFileCount);
        assertThat(jobExplorer.getJobExecution(execution.getId()).getStepExecutions())
                .singleElement()
                .satisfies(step -> assertThat(step.getWriteCount()).isEqualTo(uploadedFileCount));
        assertThat(jobExplorer.getJobExecution(execution.getId()).getStatus())
                .isEqualTo(BatchStatus.COMPLETED);
    }

    private JobParameters parameters(LocalDate targetDate) {
        return new JobParametersBuilder()
                .addLocalDate("logTargetDate", targetDate)
                .toJobParameters();
    }

    private void assertMetricCounts(double expectedTotal, double expectedUploaded) {
        assertThat(meterRegistry.get(LogMetrics.TOTAL_FILES).counter().count())
                .isEqualTo(expectedTotal);
        assertThat(meterRegistry.get(LogMetrics.UPLOADED_FILES).counter().count())
                .isEqualTo(expectedUploaded);
    }
}
