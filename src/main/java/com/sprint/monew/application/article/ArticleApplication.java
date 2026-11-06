package com.sprint.monew.application.article;

import com.sprint.monew.application.article.event.ArticleHardDeletingEvent;
import com.sprint.monew.application.article.event.ArticleSoftDeletedEvent;
import com.sprint.monew.application.article.event.ArticleViewedEvent;
import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.application.article.provided.dto.ArticleViewStatusResult;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class ArticleApplication implements ArticleUseCase {

    private final ArticleService articleService;
    private final ArticleViewStatusService articleViewStatusService;
    private final ArticleEventPublisherPort eventPublisher;
    private final IUserService userService;

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
        return List.of(ArticleSourceType.values());
    }

    @Override
    @Transactional
    public void softDeleteArticle(UUID articleId) {
        articleService.softDelete(articleId);
        eventPublisher.publish(new ArticleSoftDeletedEvent(articleId));
    }

    @Override
    @Transactional
    public void hardDeleteArticle(UUID articleId) {
        eventPublisher.publish(new ArticleHardDeletingEvent(articleId));
        articleService.hardDelete(articleId);
    }

    @Override
    @Transactional
    public ArticleViewStatusResult register(UUID articleId) {
        UUID requestUserId = RequestIdContext.get();

        User user = userService.getActiveUser(requestUserId);
        Article article = articleService.findById(articleId);

        boolean inserted = articleViewStatusService.register(user, article);

        if (inserted) {
            articleService.increaseViewCount(articleId);
        }

        ArticleViewStatus articleViewStatus =
                articleViewStatusService.findByUserIdAndArticleId(requestUserId, articleId);

        ArticleViewStatusResult result = ArticleViewStatusResult.from(articleViewStatus);
        // 처음 볼 때·다시 볼 때 모두 활동 내역의 "최근 본 기사"를 갱신한다
        Instant viewedAt =
                articleViewStatus.getLastViewedAt().atZone(ZoneId.systemDefault()).toInstant();
        eventPublisher.publish(ArticleViewedEvent.of(result, viewedAt));
        return result;
    }
}
