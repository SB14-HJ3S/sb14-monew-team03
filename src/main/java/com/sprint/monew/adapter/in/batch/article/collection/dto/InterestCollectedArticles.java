package com.sprint.monew.adapter.in.batch.article.collection.dto;

import com.sprint.monew.application.article.provided.usecase.dto.CollectedArticle;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import lombok.Getter;

@Getter
public class InterestCollectedArticles {

    private final Interest interest;
    private final List<CollectedArticle> articles;
    private int savedCount;

    private InterestCollectedArticles(Interest interest, List<CollectedArticle> articles) {
        this.interest = interest;
        this.articles = articles;
    }

    public static InterestCollectedArticles of(Interest interest, List<CollectedArticle> articles) {
        return new InterestCollectedArticles(interest, articles);
    }

    public int articleSize() {
        return articles.size();
    }

    public void recordSavedCount(int savedCount) {
        this.savedCount = savedCount;
    }
}
