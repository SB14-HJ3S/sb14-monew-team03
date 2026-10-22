package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepositoryPort {
    // JPA 기본 메서드
    Article save(Article entity);

    Optional<Article> findById(UUID id);

    List<Article> findAll();

    void deleteById(UUID id);

    // 본인 쓸 거
    List<Article> findByQuery(ArticleQuery query);

    long countByQuery(ArticleQuery query);
}
