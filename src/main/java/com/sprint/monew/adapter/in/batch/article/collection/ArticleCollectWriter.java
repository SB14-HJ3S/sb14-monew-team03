package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.adapter.in.batch.article.collection.dto.InterestCollectedArticles;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
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
    private final ArticleEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public void write(Chunk<? extends InterestCollectedArticles> chunk) {
        for (InterestCollectedArticles collected : chunk) {
            Interest interest = collected.interest();
            int newArticleCount = useCase.saveArticles(interest, collected.articles());

            //            eventPublisher.publish(
            //                new ArticlesSavedEvent(interest.getId(), interest.getName(),
            // newArticleCount));
        }
    }
}
