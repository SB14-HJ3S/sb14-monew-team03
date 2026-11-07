package com.sprint.monew.adapter.in.batch.article.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.adapter.in.batch.article.collection.dto.InterestCollectedArticles;
import com.sprint.monew.application.article.provided.usecase.ArticleCollectionUseCase;
import com.sprint.monew.domain.interest.entity.Interest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.item.Chunk;

class ArticleCollectionTimingBoundaryTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void measureStorageUntilChunkCommitOrRollback(boolean committed) {
        var registry = new SimpleMeterRegistry();
        try {
            var timing = mock(ArticleCollectionTiming.class);
            var useCase = mock(ArticleCollectionUseCase.class);
            var listener = new ArticleCollectionChunkListener(registry, timing);
            var writer = new ArticleCollectWriter(useCase, listener, timing);
            var interest = Interest.create("기술", List.of("AI"));
            when(useCase.saveArticles(interest, List.of())).thenReturn(2);
            listener.beforeChunk(null);

            writer.write(new Chunk<>(List.of(InterestCollectedArticles.of(interest, List.of()))));

            var order = inOrder(timing, useCase);
            order.verify(timing).startStorage();
            order.verify(useCase).saveArticles(interest, List.of());
            verify(timing, never()).finishStorage();

            if (committed) {
                listener.afterChunk(null);
            } else {
                listener.afterChunkError(null);
            }

            order.verify(timing).finishStorage();
            var saved = registry.find(ArticleCollectionMetrics.SAVED_ARTICLES).counter();
            assertThat(saved == null ? 0 : saved.count()).isEqualTo(committed ? 2 : 0);
        } finally {
            registry.close();
        }
    }
}
