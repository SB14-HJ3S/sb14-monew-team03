package com.sprint.monew.adapter.out.repository.article;

import com.sprint.monew.application.article.required.ArticleViewStatusRepositoryPort;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    @Override
    public Optional<ArticleViewStatus> findByUserIdAndArticleId(UUID userId, UUID articleId) {
        return jpaRepository.findByUserIdAndArticleId(userId, articleId);
    }

    @Override
    public boolean insertIfAbsent(UUID userId, UUID articleId, LocalDateTime viewedAt) {
        return jpaRepository.insertIfAbsent(
                        UUID.randomUUID(), Instant.now(), userId, articleId, viewedAt)
                == 1;
    }

    @Override
    public void updateLastViewedAt(UUID userId, UUID articleId, LocalDateTime viewedAt) {
        jpaRepository.updateLastViewedAt(userId, articleId, viewedAt);
    }
}
