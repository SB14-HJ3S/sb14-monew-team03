package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.interest.provided.dto.InterestKeywordResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class ArticleCollectReader implements ItemReader<InterestKeywordResult> {

    private final ArticleCollectionUseCase useCase;

    private ListItemReader<InterestKeywordResult> targets;

    @Override
    public InterestKeywordResult read() {
        if (targets == null) {
            targets = new ListItemReader<>(useCase.findValidInterestKeywords());
        }

        return targets.read();
    }
}
