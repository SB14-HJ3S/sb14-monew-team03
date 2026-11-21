package com.sprint.monew.application.article.provided.service;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.domain.user.entity.User;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface ArticleViewStatusService {

    Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds);

    boolean register(User user, Article article);

    ArticleViewStatus findByUserIdAndArticleId(UUID userId, UUID articleId);
}
