package com.sprint.monew.application.article;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ArticleInterestService {
    boolean createIfAbsent(Article article, Interest interest);

    void deleteAllByInterestId(UUID interestId);

    Map<UUID, List<UUID>> findInterestIdsByArticleIds(List<UUID> articleIds);
}
