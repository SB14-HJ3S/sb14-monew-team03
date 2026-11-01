package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.pagination.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface ArticleService {

    Article findById(UUID articleId);

    PageResult<Article> findPageByQuery(ArticleQuery query);

    Optional<Article> findBySourceUrl(String sourceUrl);

    Article save(Article article);

    void increaseCommentCount(UUID articleId);
}
