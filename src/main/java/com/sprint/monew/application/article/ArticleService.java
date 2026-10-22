package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.pagination.PageResult;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleService implements IArticleService {

    private final ArticleRepositoryPort repository;

    @Override
    public PageResult<Article> findPageByQuery(ArticleQuery query) {
        List<Article> articleList = repository.findByQuery(query);
        long numArticles = repository.countByQuery(query);

        boolean hasNext = articleList.size() > query.limit();

        List<Article> content =
                List.copyOf(hasNext ? articleList.subList(0, query.limit()) : articleList);

        String nextCursor = null;
        Instant nextAfter = null;

        if (hasNext) {
            Article lastArticle = content.get(content.size() - 1);
            nextCursor = getCursorValue(lastArticle, query.orderBy());
            nextAfter = lastArticle.getCreatedAt();
        }

        return new PageResult<>(
                content, nextCursor, nextAfter, content.size(), numArticles, hasNext);
    }

    private String getCursorValue(Article article, ArticleQuery.SortOrder sortField) {
        return switch (sortField) {
            case PUBLISH_DATE -> article.getPublishedAt().toString();
            case COMMENT_COUNT -> Integer.toString(article.getCommentCount());
            case VIEW_COUNT -> Integer.toString(article.getViewCount());
        };
    }
}
