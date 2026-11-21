package com.sprint.monew.adapter.in.batch.log;

import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class LogBackupListener implements ChunkListener, ItemWriteListener<List<Path>> {

    private final MeterRegistry meterRegistry;

    private long chunkFileCount;
    private long chunkUploadedCount;

    @Override
    public void beforeChunk(ChunkContext context) {
        chunkFileCount = 0;
        chunkUploadedCount = 0;
    }

    @Override
    public void beforeWrite(Chunk<? extends List<Path>> items) {
        for (List<Path> files : items) {
            chunkFileCount += files.size();
        }
    }

    @Override
    public void afterWrite(Chunk<? extends List<Path>> items) {
        for (List<Path> files : items) {
            chunkUploadedCount += files.size();
        }
    }

    @Override
    public void afterChunk(ChunkContext context) {
        recordCounts(chunkFileCount, chunkUploadedCount);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        Throwable failure = (Throwable) context.getAttribute(ROLLBACK_EXCEPTION_KEY);
        recordCounts(chunkFileCount, 0);

        StepExecution stepExecution = context.getStepContext().getStepExecution();
        log.warn(
                "로그 백업 청크 실패: jobExecutionId={}, targetDate={}, nextPartNumber={}",
                stepExecution.getJobExecutionId(),
                stepExecution.getJobParameters().getLocalDate("logTargetDate"),
                stepExecution.getExecutionContext().getLong(LogBackupWriter.NEXT_PART_NUMBER, 1L),
                failure);
    }

    private void recordCounts(long total, long uploaded) {
        meterRegistry.counter(LogMetrics.TOTAL_FILES).increment(total);
        meterRegistry.counter(LogMetrics.UPLOADED_FILES).increment(uploaded);
    }
}
