package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import java.util.List;

public interface ArticleQueryRepository {

    List<Article> findByQuery(ArticleQuery query);

    long countByQuery(ArticleQuery query);
}
