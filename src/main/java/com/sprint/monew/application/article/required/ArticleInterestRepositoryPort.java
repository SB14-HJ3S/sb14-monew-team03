package com.sprint.monew.application.article.required;

import com.sprint.monew.domain.article.entity.ArticleInterest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleInterestRepositoryPort {

    void saveAll(List<ArticleInterest> links);

    ArticleInterest save(ArticleInterest articleInterest);

    Optional<ArticleInterest> findByArticleIdAndInterestId(UUID articleId, UUID interestId);

    void deleteAllByInterestId(UUID interestId);

    List<ArticleInterest> findAllByArticleIds(List<UUID> articleIds);
}
