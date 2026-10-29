package com.sprint.monew.application.article;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.UUID;

public interface ArticleInterestService {
    ArticleInterest getOrCreateArticleInterest(Article article, Interest interest);

    void deleteAllByInterestId(UUID interestId);
}
