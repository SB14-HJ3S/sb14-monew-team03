package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@ApplicationService
@RequiredArgsConstructor
public class ArticleApplication implements ArticleUseCase {

    private final IArticleService articleService;
    private final IArticleViewStatusService articleViewStatusService;

    @Override
    public PageResult<ArticleResult> findArticlePage(ArticleQuery query) {
        UUID requestUserId = RequestIdContext.get();

        PageResult<Article> articles = articleService.findPageByQuery(query);
        List<UUID> articleIds = articles.content().stream().map(Article::getId).toList();
        Set<UUID> viewedArticleIds =
                articleViewStatusService.findViewedArticleIds(requestUserId, articleIds);

        List<ArticleResult> content =
                articles.content().stream()
                        .map(
                                article ->
                                        ArticleResult.from(
                                                article,
                                                viewedArticleIds.contains(article.getId())))
                        .toList();
        return new PageResult<>(
                content,
                articles.nextCursor(),
                articles.nextAfter(),
                articles.size(),
                articles.totalElements(),
                articles.hasNext());
    }

    @Override
    public ArticleResult findById(UUID articleId) {
        UUID requestUserId = RequestIdContext.get();

        Article article = articleService.findById(articleId);
        boolean viewedByMe =
                articleViewStatusService
                        .findViewedArticleIds(requestUserId, List.of(articleId))
                        .contains(articleId);

        return ArticleResult.from(article, viewedByMe);
    }

    @Override
    public List<ArticleSourceType> findSourceTypes() {
        return List.of();
    }

    @Override
    public void softDeleteArticle(UUID articleId) {}

    @Override
    public void hardDeleteArticle(UUID articleId) {}

    @Override
    public List<Article> restoreArticles(LocalDateTime from, LocalDateTime to) {
        return List.of();
    }

    @Override
    public ArticleViewStatus register(UUID articleId, UUID requestId) {
        return null;
    }
}
