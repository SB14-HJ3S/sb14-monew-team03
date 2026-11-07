package com.sprint.monew.application.article.provided.service;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ArticleInterestService {
    boolean createIfAbsent(Article article, Interest interest);

    Set<UUID> findLinkedArticleIds(UUID interestId, List<UUID> articleIds);

    void createLinks(Interest interest, List<Article> articles);

    void deleteAllByInterestId(UUID interestId);

    Map<UUID, List<UUID>> findInterestIdsByArticleIds(List<UUID> articleIds);
}
