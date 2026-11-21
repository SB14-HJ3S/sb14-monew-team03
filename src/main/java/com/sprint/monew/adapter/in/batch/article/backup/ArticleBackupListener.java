package com.sprint.monew.adapter.in.batch.article.backup;

import com.sprint.monew.application.article.provided.usecase.dto.ArticleResult;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
public class ArticleBackupListener
        implements ChunkListener, ItemWriteListener<List<ArticleResult>> {

    private final MeterRegistry meterRegistry;

    private long chunkUploadedCount;

    @Override
    public void afterWrite(Chunk<? extends List<ArticleResult>> items) {
        for (List<ArticleResult> articles : items) {
            chunkUploadedCount += articles.size();
        }
    }

    @Override
    public void beforeChunk(@NonNull ChunkContext context) {
        chunkUploadedCount = 0;
    }

    @Override
    public void afterChunk(@NonNull ChunkContext context) {
        meterRegistry.counter(ArticleBackupMetrics.UPLOADED_ARTICLES).increment(chunkUploadedCount);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        Throwable throwable = (Throwable) context.getAttribute(ROLLBACK_EXCEPTION_KEY);
        StepExecution stepExecution = context.getStepContext().getStepExecution();
        log.warn(
                "기사 백업 청크 실패: jobExecutionId={}, targetDate={}, nextPartNumber={}",
                stepExecution.getJobExecutionId(),
                stepExecution.getJobParameters().getLocalDate("targetDate"),
                stepExecution
                        .getExecutionContext()
                        .getLong(ArticleBackupWriter.NEXT_PART_NUMBER, 1L),
                throwable);
    }
}
