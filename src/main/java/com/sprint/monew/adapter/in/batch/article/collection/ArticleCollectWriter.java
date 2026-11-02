package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.adapter.in.batch.article.collection.dto.InterestCollectedArticles;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.domain.interest.entity.Interest;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ArticleCollectWriter implements ItemWriter<InterestCollectedArticles> {

    private final ArticleCollectionUseCase useCase;
    private final ArticleCollectionChunkListener listener;

    @Override
    @Transactional
    public void write(Chunk<? extends InterestCollectedArticles> chunk) {
        for (InterestCollectedArticles collected : chunk) {
            Interest interest = collected.interest();
            int newArticleCount = useCase.saveArticles(interest, collected.articles());
            listener.addSavedCount(newArticleCount);
        }
    }
}
