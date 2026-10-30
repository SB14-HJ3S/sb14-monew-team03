package com.sprint.monew.application.article;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.application.article.required.NaverNewsPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.pagination.PageResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepositoryPort repository;
    private final NaverNewsPort naverNewsPort;
    private final ArticleEventPublisherPort eventPublisherPort;

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
    public List<CollectedArticle> collectNaverArticles(String keyword) {
        return naverNewsPort.search(keyword);
    }

    @Override
    public Optional<Article> saveCollectedArticle(CollectedArticle collectedArticle) {

        Optional<Article> existing = repository.findBySourceUrl(collectedArticle.sourceUrl());
        if (existing.isPresent()) { // 이미 수집했던 기사
            Article article = existing.get();

            if (article.isDeleted()) { // 논리 삭제 했었던 기사
                return Optional.empty();
            }

            return Optional.of(article);
        }

        Article article =
                Article.create(
                        collectedArticle.source(),
                        collectedArticle.sourceUrl(),
                        collectedArticle.title(),
                        collectedArticle.publishedAt(),
                        collectedArticle.summary());

        return Optional.of(repository.save(article));
    }

    @Override
    @Transactional
    public void increaseCommentCount(UUID articleId) {
        repository.increaseCommentCount(articleId);
    }

    @Override
    public Optional<Article> findBySourceUrl(String sourceUrl) {
        return repository.findBySourceUrl(sourceUrl);
    }

    @Override
    public Article createCollectedArticle(CollectedArticle collected) {
        Article article =
                Article.create(
                        collected.source(),
                        collected.sourceUrl(),
                        collected.title(),
                        collected.publishedAt(),
                        collected.summary());

        return repository.save(article);
    }

    @Override
    public void publishArticlesSaved(UUID interestId, String interestName, int articleCount) {
        eventPublisherPort.publish(new ArticlesSavedEvent(interestId, interestName, articleCount));
    }

    private String getCursorValue(Article article, ArticleQuery.SortOrder sortField) {
        return switch (sortField) {
            case PUBLISH_DATE -> article.getPublishedAt().toString();
            case COMMENT_COUNT -> Integer.toString(article.getCommentCount());
            case VIEW_COUNT -> Integer.toString(article.getViewCount());
        };
    }
}
