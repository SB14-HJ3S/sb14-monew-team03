package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleFinder;
import com.sprint.monew.application.article.provided.ArticleRemover;
import com.sprint.monew.application.article.provided.ArticleRestorer;
import com.sprint.monew.application.article.provided.ArticleViewStatusRegister;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ArticleService
        implements ArticleFinder, ArticleRemover, ArticleRestorer, ArticleViewStatusRegister {

    @Override
    public PageResult<Article> findQuery() {
        return null;
    }

    @Override
    public Article findById(UUID requestId, UUID articleId) {
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
    public ArticleViewStatus register(UUID requestId, UUID articleId) {
        return null;
    }
}
