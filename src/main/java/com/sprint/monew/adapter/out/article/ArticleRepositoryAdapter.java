package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleRepositoryAdapter implements ArticleRepositoryPort {

    private final ArticleJpaRepository jpaRepository;

    @Override
    public Article save(Article entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<Article> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Article> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Article> findByQuery(ArticleQuery query) {
        return jpaRepository.findByQuery(query);
    }

    @Override
    public long countByQuery(ArticleQuery query) {
        return jpaRepository.countByQuery(query);
    }
}
