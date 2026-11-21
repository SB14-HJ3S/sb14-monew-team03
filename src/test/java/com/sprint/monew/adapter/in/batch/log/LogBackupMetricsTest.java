package com.sprint.monew.adapter.in.batch.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.Chunk;

class LogBackupMetricsTest {

    @ParameterizedTest
    @CsvSource({"COMPLETED, 3, 3", "FAILED, 3, 0", "COMPLETED, 0, 0"})
    @DisplayName("묶음 업로드 성공과 실패의 파일 수를 집계하고 기존 종료 상태를 유지한다")
    void publishBackupResultRegardlessOfChunkStatus(
            BatchStatus status, int expectedTotal, int expectedUploaded) {
        LocalDate date = LocalDate.of(2026, 10, 31);
        StepExecution step = step(date);
        step.setStatus(status);
        ExitStatus exitStatus =
                status == BatchStatus.FAILED ? ExitStatus.FAILED : ExitStatus.COMPLETED;
        step.setExitStatus(exitStatus);
        ChunkContext context = new ChunkContext(new StepContext(step));
        List<Path> files =
                IntStream.range(0, expectedTotal)
                        .mapToObj(index -> Path.of("monew.log." + date + "." + index + ".log"))
                        .toList();
        Chunk<List<Path>> chunk = files.isEmpty() ? new Chunk<>() : new Chunk<>(List.of(files));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            LogBackupListener listener = new LogBackupListener(registry);
            listener.beforeChunk(context);
            listener.beforeWrite(chunk);
            if (status == BatchStatus.FAILED) {
                context.setAttribute(
                        ChunkListener.ROLLBACK_EXCEPTION_KEY,
                        new LogBackupFailedException(date, expectedTotal, expectedUploaded));
                listener.afterChunkError(context);
            } else {
                listener.afterWrite(chunk);
                assertThat(registry.find(LogMetrics.UPLOADED_FILES).counter()).isNull();
                listener.afterChunk(context);
            }

            assertThat(step.getStatus()).isEqualTo(status);
            assertThat(step.getExitStatus()).isEqualTo(exitStatus);
            assertThat(registry.get(LogMetrics.TOTAL_FILES).counter().count())
                    .isEqualTo(expectedTotal);
            assertThat(registry.get(LogMetrics.UPLOADED_FILES).counter().count())
                    .isEqualTo(expectedUploaded);
        } finally {
            registry.close();
        }
    }

    @Test
    @DisplayName("업로드 이후 커밋이 실패하면 성공 파일 수를 확정하지 않는다")
    void doNotCountSuccessfulUploadBeforeCommit() {
        ChunkContext context = new ChunkContext(new StepContext(step(LocalDate.of(2026, 10, 31))));
        Chunk<List<Path>> chunk =
                new Chunk<>(List.of(List.of(Path.of("monew.log.2026-10-31.0.log"))));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            LogBackupListener listener = new LogBackupListener(registry);
            listener.beforeChunk(context);
            listener.beforeWrite(chunk);
            listener.afterWrite(chunk);
            context.setAttribute(
                    ChunkListener.ROLLBACK_EXCEPTION_KEY,
                    new IllegalStateException("commit failed"));
            listener.afterChunkError(context);

            assertThat(registry.get(LogMetrics.TOTAL_FILES).counter().count()).isEqualTo(1);
            assertThat(registry.get(LogMetrics.UPLOADED_FILES).counter().count()).isZero();
        } finally {
            registry.close();
        }
    }

    private StepExecution step(LocalDate date) {
        var parameters =
                new JobParametersBuilder().addLocalDate("logTargetDate", date).toJobParameters();
        return new JobExecution(1L, parameters).createStepExecution("logBackupStep");
    }
}
