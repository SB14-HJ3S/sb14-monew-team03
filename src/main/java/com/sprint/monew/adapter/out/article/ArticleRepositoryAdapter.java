package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    @Override
    public Optional<Article> findBySourceUrl(String sourceUrl) {
        return jpaRepository.findBySourceUrl(sourceUrl);
    }

    @Override
    public List<Article> findAllBySourceUrlIn(Collection<String> sourceUrls) {
        return jpaRepository.findAllBySourceUrlIn(sourceUrls);
    }

    @Override
    public int increaseCommentCount(UUID articleId) {
        return jpaRepository.increaseCommentCount(articleId);
    }

    @Override
    public int decreaseCommentCount(UUID articleId) {
        return jpaRepository.decreaseCommentCount(articleId);
    }

    @Override
    public int decreaseCommentCountsByUserIds(List<UUID> userIds) {
        return jpaRepository.decreaseCommentCountsByUserIds(userIds);
    }

    @Override
    public int increaseViewCount(UUID articleId) {
        return jpaRepository.increaseViewCount(articleId);
    }

    @Override
    public int softDeleteIfActive(UUID articleId, Instant deletedAt) {
        return jpaRepository.softDeleteIfActive(articleId, deletedAt);
    }

    @Override
    public int hardDeleteById(UUID articleId) {
        return jpaRepository.hardDeleteById(articleId);
    }

    @Override
    public List<Article> findBackupChunk(
            LocalDateTime from, LocalDateTime to, UUID afterId, int limit) {
        return jpaRepository.findBackupChunk(from, to, afterId, PageRequest.of(0, limit));
    }

    @Override
    public List<Article> saveAll(List<Article> articles) {
        return jpaRepository.saveAll(articles);
    }
}
