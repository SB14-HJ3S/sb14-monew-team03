package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import com.sprint.monew.global.pagination.PageResult;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    public static final ZoneId BACKUP_ZONE = ZoneId.of("Asia/Seoul");

    private final ArticleRepositoryPort repository;

    @Override
    @Transactional(readOnly = true)
    public List<Article> getArticlesForBackup(LocalDate date, UUID afterId, int limit) {
        if (limit <= 0) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST, Map.of("limit", limit));
        }

        Instant from = date.atStartOfDay(BACKUP_ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(BACKUP_ZONE).toInstant();

        return repository.findBackupChunk(from, to, afterId, limit);
    }

    @Override
    public Article findById(UUID articleId) {
        return repository
                .findById(articleId)
                .filter(article -> !article.isDeleted())
                .orElseThrow(() -> articleNotFound(articleId));
    }

    @Override
    public PageResult<Article> findPageByQuery(ArticleQuery query) {
        return PageResult.of(
                repository.findByQuery(query),
                repository.countByQuery(query),
                query.orderBy(),
                query.limit());
    }

    @Override
    public Optional<Article> findBySourceUrl(String sourceUrl) {
        return repository.findBySourceUrl(sourceUrl);
    }

    @Override
    public Article save(Article article) {
        return repository.save(article);
    }

    @Override
    @Transactional
    public void increaseCommentCount(UUID articleId) {
        repository.increaseCommentCount(articleId);
    }

    @Override
    @Transactional
    public void decreaseCommentCount(UUID articleId) {
        repository.decreaseCommentCount(articleId);
    }

    @Override
    @Transactional
    public void softDelete(UUID articleId) {
        if (repository.softDeleteIfActive(articleId, Instant.now()) == 0) {
            throw articleNotFound(articleId);
        }
    }

    @Override
    public void hardDelete(UUID articleId) {}

    private MonewException articleNotFound(UUID articleId) {
        return new MonewException(
                ArticleErrorType.ARTICLE_NOT_FOUND, Map.of("articleId", articleId));
    }
}
