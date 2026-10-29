package com.sprint.monew.adapter.in.batch;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleCollectionRunner {

    private final ArticleCollectionUseCase collectionUseCase;

    public void runCollection() {
        List<Interest> interests = collectionUseCase.findCollectionInterests();
        for (Interest interest : interests) {
            List<CollectedArticle> articles = collectionUseCase.collectNaverArticles(interest);
            collectionUseCase.saveArticlesAndLink(articles, interest);
        }
    }
}
