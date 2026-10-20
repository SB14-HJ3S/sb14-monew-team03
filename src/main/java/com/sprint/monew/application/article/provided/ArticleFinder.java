package com.sprint.monew.application.article.provided;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.type.ArticleSourceType;
import java.util.List;
import java.util.UUID;

public interface ArticleFinder {

    List<Article> findAll();

    Article findById(UUID requestId, UUID articleId);

    List<ArticleSourceType> findSourceTypes();
}
