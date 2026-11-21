package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.adapter.in.batch.article.collection.dto.InterestCollectedArticles;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class ArticleCollectionListener
        implements ChunkListener, ItemWriteListener<InterestCollectedArticles> {

    private final MeterRegistry meterRegistry;
    private final ArticleCollectionTiming timing;
    private long chunkSavedCount;

    @Override
    public void beforeChunk(ChunkContext context) {
        chunkSavedCount = 0;
    }

    @Override
    public void afterWrite(Chunk<? extends InterestCollectedArticles> items) {
        for (InterestCollectedArticles result : items) {
            chunkSavedCount += result.getSavedCount();
        }
    }

    @Override
    public void afterChunk(ChunkContext context) {
        timing.finishStorage();
        meterRegistry.counter(ArticleCollectionMetrics.SAVED_ARTICLES).increment(chunkSavedCount);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        timing.finishStorage();
    }
}
