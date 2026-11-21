package com.sprint.monew.adapter.in.batch.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.log.provided.usecase.LogBackupUseCase;
import com.sprint.monew.application.log.provided.usecase.dto.LogBackupResult;
import com.sprint.monew.application.log.provided.usecase.dto.LogResult;
import com.sprint.monew.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@IntegrationTest
@TestPropertySource(properties = "monew.log.backup.limit=3")
class LogBackupJobIntegrationTest {

    private static final int LIMIT = 3;

    @MockitoBean private LogBackupUseCase useCase;
    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobExplorer jobExplorer;
    @Autowired private MeterRegistry registry;

    @Autowired
    @Qualifier("logBackupJob")
    private Job job;

    @Test
    @DisplayName("첫 묶음 업로드에 실패하면 커서를 진행하지 않고 성공 파일 수를 0으로 집계한다")
    void failArchiveWithoutAdvancingCursor() throws Exception {
        int uploaded = 0;
        LocalDate date = LocalDate.of(2001, 1, 1);
        List<Path> files = files(date, 0, 1, 2);
        when(useCase.readLogs(date, -1, LIMIT)).thenReturn(LogResult.from(files));
        when(useCase.backup(date, 1L, files)).thenReturn(new LogBackupResult(3, uploaded));
        double totalBefore = totalFiles();
        double uploadedBefore = uploadedFiles();

        JobExecution failed = jobLauncher.run(job, parameters(date));

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failed.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
        assertThat(totalFiles()).isEqualTo(totalBefore + 3);
        assertThat(uploadedFiles()).isEqualTo(uploadedBefore + uploaded);
        assertThat(failed.getAllFailureExceptions())
                .anySatisfy(
                        failure ->
                                assertThat(failure)
                                        .isInstanceOf(LogBackupFailedException.class)
                                        .hasMessageContaining("targetDate=" + date)
                                        .hasMessageContaining("uploadedFiles=" + uploaded));
        assertCheckpoint(failed, -1, 1L, 0L);
        verify(useCase, never()).readLogs(date, 2, LIMIT);
    }

    @Test
    @DisplayName("대상 파일이 없으면 업로드 없이 완료한다")
    void completeEmptyBackup() throws Exception {
        LocalDate date = LocalDate.of(2001, 2, 1);
        when(useCase.readLogs(date, -1, LIMIT)).thenReturn(LogResult.from(List.of()));
        double totalBefore = totalFiles();
        double uploadedBefore = uploadedFiles();

        JobExecution completed = jobLauncher.run(job, parameters(date));

        assertThat(completed.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(totalFiles()).isEqualTo(totalBefore);
        assertThat(uploadedFiles()).isEqualTo(uploadedBefore);
        assertCheckpoint(completed, -1, 1L, 0L);
        verify(useCase).readLogs(date, -1, LIMIT);
        verifyNoMoreInteractions(useCase);
    }

    @Test
    @DisplayName("첫 조회에 실패하면 파일 수를 증가시키지 않는다")
    void failBeforeReadingFiles() throws Exception {
        LocalDate date = LocalDate.of(2001, 3, 1);
        IOException failure = new IOException("로그 디렉터리 조회 실패");
        when(useCase.readLogs(date, -1, LIMIT)).thenThrow(failure);
        double totalBefore = totalFiles();
        double uploadedBefore = uploadedFiles();

        JobExecution failed = jobLauncher.run(job, parameters(date));

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failed.getAllFailureExceptions()).contains(failure);
        assertThat(totalFiles()).isEqualTo(totalBefore);
        assertThat(uploadedFiles()).isEqualTo(uploadedBefore);
        assertCheckpoint(failed, -1, 1L, 0L);
    }

    @Test
    @DisplayName("성공한 묶음 이후 업로드에 실패하면 마지막 커서와 part 번호부터 재시작한다")
    void restartFromFailedChunk() throws Exception {
        LocalDate date = LocalDate.of(2001, 4, 1);
        List<Path> first = files(date, 0, 2, 10);
        List<Path> second = files(date, 11, 14);
        when(useCase.readLogs(date, -1, LIMIT)).thenReturn(LogResult.from(first));
        when(useCase.readLogs(date, 10, LIMIT)).thenReturn(LogResult.from(second));
        when(useCase.readLogs(date, 14, LIMIT)).thenReturn(LogResult.from(List.of()));
        when(useCase.backup(date, 1L, first)).thenReturn(success(first));
        when(useCase.backup(date, 2L, second))
                .thenReturn(new LogBackupResult(2, 0))
                .thenReturn(success(second));
        double totalBefore = totalFiles();
        double uploadedBefore = uploadedFiles();
        JobParameters parameters = parameters(date);

        JobExecution failed = jobLauncher.run(job, parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertCheckpoint(failed, 10, 2L, 1L);
        assertThat(totalFiles()).isEqualTo(totalBefore + 5);
        assertThat(uploadedFiles()).isEqualTo(uploadedBefore + 3);
        verify(useCase, never()).readLogs(date, 14, LIMIT);

        JobExecution restarted = jobLauncher.run(job, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getJobInstance().getInstanceId())
                .isEqualTo(failed.getJobInstance().getInstanceId());
        assertCheckpoint(restarted, 14, 3L, 1L);
        assertThat(totalFiles()).isEqualTo(totalBefore + 7);
        assertThat(uploadedFiles()).isEqualTo(uploadedBefore + 5);
        verify(useCase).readLogs(date, -1, LIMIT);
        verify(useCase).backup(date, 1L, first);
        verify(useCase, times(2)).backup(date, 2L, second);
    }

    @Test
    @DisplayName("첫 청크 업로드 실패 후에는 커서 없이 part 1부터 다시 처리한다")
    void restartAfterFirstChunkFailure() throws Exception {
        LocalDate date = LocalDate.of(2001, 5, 1);
        List<Path> files = files(date, 0, 1);
        when(useCase.readLogs(date, -1, LIMIT)).thenReturn(LogResult.from(files));
        when(useCase.readLogs(date, 1, LIMIT)).thenReturn(LogResult.from(List.of()));
        when(useCase.backup(date, 1L, files))
                .thenReturn(new LogBackupResult(2, 0))
                .thenReturn(success(files));
        JobParameters parameters = parameters(date);

        JobExecution failed = jobLauncher.run(job, parameters);
        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertCheckpoint(failed, -1, 1L, 0L);

        JobExecution restarted = jobLauncher.run(job, parameters);
        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertCheckpoint(restarted, 1, 2L, 1L);
        verify(useCase, times(2)).readLogs(date, -1, LIMIT);
        verify(useCase, times(2)).backup(date, 1L, files);
    }

    @Test
    @DisplayName("다음 조회에 실패하면 커밋된 청크를 유지하고 마지막 커서부터 재시작한다")
    void restartAfterReadFailure() throws Exception {
        LocalDate date = LocalDate.of(2001, 6, 1);
        List<Path> first = files(date, 0, 2, 10);
        List<Path> second = files(date, 11);
        when(useCase.readLogs(date, -1, LIMIT)).thenReturn(LogResult.from(first));
        when(useCase.readLogs(date, 10, LIMIT))
                .thenThrow(new IOException("다음 조회 실패"))
                .thenReturn(LogResult.from(second));
        when(useCase.readLogs(date, 11, LIMIT)).thenReturn(LogResult.from(List.of()));
        when(useCase.backup(date, 1L, first)).thenReturn(success(first));
        when(useCase.backup(date, 2L, second)).thenReturn(success(second));
        JobParameters parameters = parameters(date);

        JobExecution failed = jobLauncher.run(job, parameters);
        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertCheckpoint(failed, 10, 2L, 1L);

        JobExecution restarted = jobLauncher.run(job, parameters);
        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertCheckpoint(restarted, 11, 3L, 1L);
        verify(useCase).backup(date, 1L, first);
        verify(useCase, times(2)).readLogs(date, 10, LIMIT);
        verify(useCase).backup(date, 2L, second);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("첫 청크 또는 두 번째 청크의 DB 커밋 실패 시 마지막 커밋 위치를 유지한다")
    void restartAfterCommitFailure(boolean firstChunkCommitted) throws Exception {
        LocalDate date = LocalDate.of(2001, 7, firstChunkCommitted ? 2 : 1);
        List<Path> first = files(date, 0, 2, 10);
        List<Path> second = files(date, 11, 12);
        when(useCase.readLogs(date, -1, LIMIT)).thenReturn(LogResult.from(first));
        List<Path> failingPage = firstChunkCommitted ? second : first;
        long failingPart = firstChunkCommitted ? 2L : 1L;
        if (firstChunkCommitted) {
            when(useCase.backup(date, 1L, first)).thenReturn(success(first));
            when(useCase.readLogs(date, 10, LIMIT)).thenReturn(LogResult.from(second));
            when(useCase.readLogs(date, 12, LIMIT)).thenReturn(LogResult.from(List.of()));
        } else {
            when(useCase.readLogs(date, 10, LIMIT)).thenReturn(LogResult.from(List.of()));
        }
        when(useCase.backup(date, failingPart, failingPage))
                .thenAnswer(
                        invocation -> {
                            TransactionSynchronizationManager.registerSynchronization(
                                    new TransactionSynchronization() {
                                        @Override
                                        public void beforeCommit(boolean readOnly) {
                                            throw new IllegalStateException("로그 백업 청크 커밋 실패");
                                        }
                                    });
                            return success(failingPage);
                        })
                .thenReturn(success(failingPage));
        double uploadedBefore = uploadedFiles();
        JobParameters parameters = parameters(date);

        JobExecution failed = jobLauncher.run(job, parameters);
        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertCheckpoint(
                failed, firstChunkCommitted ? 10 : -1, failingPart, firstChunkCommitted ? 1L : 0L);
        assertThat(uploadedFiles()).isEqualTo(uploadedBefore + (firstChunkCommitted ? 3 : 0));

        JobExecution restarted = jobLauncher.run(job, parameters);
        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertCheckpoint(restarted, firstChunkCommitted ? 12 : 10, failingPart + 1, 1L);
        assertThat(uploadedFiles()).isEqualTo(uploadedBefore + (firstChunkCommitted ? 5 : 3));
        verify(useCase, times(2)).backup(date, failingPart, failingPage);
        if (firstChunkCommitted) {
            verify(useCase).backup(date, 1L, first);
        }
    }

    @Test
    @DisplayName("완료된 날짜는 재실행하지 않고 다음 날짜는 커서와 part 번호를 새로 시작한다")
    void rejectCompletedDateAndStartNextDate() throws Exception {
        LocalDate date = LocalDate.of(2001, 8, 1);
        LocalDate nextDate = date.plusDays(1);
        for (LocalDate target : List.of(date, nextDate)) {
            List<Path> files = files(target, 0);
            when(useCase.readLogs(target, -1, LIMIT)).thenReturn(LogResult.from(files));
            when(useCase.readLogs(target, 0, LIMIT)).thenReturn(LogResult.from(List.of()));
            when(useCase.backup(target, 1L, files)).thenReturn(success(files));
        }

        assertThat(jobLauncher.run(job, parameters(date)).getStatus())
                .isEqualTo(BatchStatus.COMPLETED);
        assertThatThrownBy(() -> jobLauncher.run(job, parameters(date)))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
        JobExecution nextDay = jobLauncher.run(job, parameters(nextDate));
        assertThat(nextDay.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertCheckpoint(nextDay, 0, 2L, 1L);
        verify(useCase).readLogs(date, -1, LIMIT);
        verify(useCase).readLogs(nextDate, -1, LIMIT);
        verify(useCase).backup(nextDate, 1L, files(nextDate, 0));
    }

    private void assertCheckpoint(
            JobExecution execution, int index, long nextPart, long writtenChunks) {
        assertThat(jobExplorer.getJobExecution(execution.getId()).getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(
                                            step.getExecutionContext()
                                                    .getInt(LogBackupReader.AFTER_INDEX, -1))
                                    .isEqualTo(index);
                            assertThat(step.getExecutionContext().getInt(LogBackupReader.LIMIT))
                                    .isEqualTo(LIMIT);
                            assertThat(
                                            step.getExecutionContext()
                                                    .getLong(LogBackupWriter.NEXT_PART_NUMBER))
                                    .isEqualTo(nextPart);
                            assertThat(step.getWriteCount()).isEqualTo(writtenChunks);
                        });
    }

    private JobParameters parameters(LocalDate date) {
        return new JobParametersBuilder().addLocalDate("logTargetDate", date).toJobParameters();
    }

    private List<Path> files(LocalDate date, int... indices) {
        return Arrays.stream(indices)
                .mapToObj(index -> Path.of("monew.log." + date + "." + index + ".log"))
                .toList();
    }

    private LogBackupResult success(List<Path> files) {
        return new LogBackupResult(files.size(), files.size());
    }

    private double totalFiles() {
        return registry.counter(LogMetrics.TOTAL_FILES).count();
    }

    private double uploadedFiles() {
        return registry.counter(LogMetrics.UPLOADED_FILES).count();
    }
}
