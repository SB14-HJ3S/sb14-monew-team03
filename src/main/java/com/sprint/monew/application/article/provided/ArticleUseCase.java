package com.sprint.monew.application.article.provided;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ArticleUseCase {
    PageResult<Article> findByQuery(ArticleQuery query);

    Article findById(UUID requestId, UUID articleId);

    List<ArticleSourceType> findSourceTypes();

    void softDeleteArticle(UUID articleId);

    void hardDeleteArticle(UUID articleId);

    List<Article> restoreArticles(LocalDateTime from, LocalDateTime to);

    ArticleViewStatus register(UUID requestId, UUID articleId);
}
