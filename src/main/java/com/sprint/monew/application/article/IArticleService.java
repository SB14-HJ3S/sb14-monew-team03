package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.pagination.PageResult;

public interface IArticleService {
    PageResult<Article> findPageByQuery(ArticleQuery query);
}
