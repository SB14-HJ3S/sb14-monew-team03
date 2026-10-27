package com.sprint.monew.application.article.provided;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;

public interface ArticleCollectionUseCase {

    List<Interest> findCollectionInterests();

    List<CollectedArticle> collectNaverArticles(Interest interest);

    void saveAndLink(CollectedArticle article, Interest interest);
}
