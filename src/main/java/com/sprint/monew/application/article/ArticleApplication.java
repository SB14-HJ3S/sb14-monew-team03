package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleListItem;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleApplication implements ArticleUseCase {

    private final IArticleService articleService;
    private final IArticleViewStatusService articleViewStatusService;

    @Override
    public PageResult<ArticleListItem> findByQuery(ArticleQuery query) {
        // 여기서 ArticleListItem 조립한다 -
        // 1 - 기사 목록 call
        PageResult<Article> articles = articleService.findByQuery(query);

        // 2 - 기사 ID List 추출
        List<UUID> articleIds = articles.content().stream().map(Article::getId).toList();

        // 3 - 기사 ID, 요청자 ID로 요청자가 조회한 기사 ID Set 추출
        Set<UUID> viewedArticleIds =
                articleViewStatusService.findViewedArticleIds(query.requestId(), articleIds);

        // 4 - ArticleListItem List 조립
        List<ArticleListItem> content =
                articles.content().stream()
                        .map(
                                article ->
                                        ArticleListItem.from(
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
    public ArticleListItem findById(UUID articleId, UUID requestId) {
        return null;
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
