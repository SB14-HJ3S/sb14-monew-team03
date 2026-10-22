package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.required.ArticleViewStatusRepositoryPort;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleViewStatusRepositoryAdapter implements ArticleViewStatusRepositoryPort {

    private final ArticleViewStatusJpaRepository jpaRepository;

    @Override
    public ArticleViewStatus save(ArticleViewStatus entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<ArticleViewStatus> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ArticleViewStatus> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return Set.of();
        }
        return jpaRepository.findViewedArticleIds(userId, articleIds);
    }
}
