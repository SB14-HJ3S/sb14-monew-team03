package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.pagination.PageResult;
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

    private final ArticleRepositoryPort repository;

    @Override
    public Article findById(UUID articleId) {
        return repository
                .findById(articleId)
                .filter(article -> !article.isDeleted())
                .orElseThrow(
                        () ->
                                new MonewException(
                                        ArticleErrorType.ARTICLE_NOT_FOUND,
                                        Map.of("articleId", articleId)));
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
}
