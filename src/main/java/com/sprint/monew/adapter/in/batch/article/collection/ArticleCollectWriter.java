package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.adapter.in.batch.article.collection.dto.InterestCollectedArticles;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.domain.interest.entity.Interest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
            int inputCount = collected.articles().size();

            int newArticleCount = useCase.saveArticles(interest, collected.articles());

            listener.addSavedCount(newArticleCount);

            log.info(
                    "기사 저장 처리: 관심사={}, 입력={}건, 신규 저장={}건",
                    interest.getName(),
                    inputCount,
                    newArticleCount);
        }
    }
}
