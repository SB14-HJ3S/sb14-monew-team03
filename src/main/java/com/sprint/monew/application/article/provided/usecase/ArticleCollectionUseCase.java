package com.sprint.monew.application.article.provided.usecase;

import com.sprint.monew.application.article.provided.usecase.dto.CollectedArticle;
import com.sprint.monew.application.interest.provided.usecase.dto.InterestKeywordResult;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.type.ArticleSourceType;
import java.util.List;

public interface ArticleCollectionUseCase {

    List<InterestKeywordResult> findValidInterestKeywords();

    List<CollectedArticle> collectNaverArticles(String keyword);

    List<CollectedArticle> collectRssArticles(ArticleSourceType source);

    int saveArticles(Interest interest, List<CollectedArticle> articles);

    boolean matchesInterest(CollectedArticle article, Interest interest);
}
