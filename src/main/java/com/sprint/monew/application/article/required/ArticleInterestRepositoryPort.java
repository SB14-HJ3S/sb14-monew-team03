package com.sprint.monew.application.article.required;

import com.sprint.monew.domain.article.entity.ArticleInterest;
import java.util.Optional;
import java.util.UUID;

public interface ArticleInterestRepositoryPort {

    ArticleInterest save(ArticleInterest articleInterest);

    Optional<ArticleInterest> findByArticleIdAndInterestId(UUID articleId, UUID interestId);

    void deleteAllByInterestId(UUID interestId);
}
