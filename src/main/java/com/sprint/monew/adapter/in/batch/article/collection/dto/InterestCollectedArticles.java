package com.sprint.monew.adapter.in.batch.article.collection.dto;

import com.sprint.monew.application.article.provided.usecase.dto.CollectedArticle;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;

public record InterestCollectedArticles(Interest interest, List<CollectedArticle> articles) {

    public static InterestCollectedArticles of(Interest interest, List<CollectedArticle> articles) {
        return new InterestCollectedArticles(interest, articles);
    }
}
