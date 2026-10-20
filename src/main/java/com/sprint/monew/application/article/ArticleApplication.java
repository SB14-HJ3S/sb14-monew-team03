package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleApplication implements ArticleUseCase {

    @Override
    public PageResult<Article> findByQuery(ArticleQuery query) {
        return null;
    }

    @Override
    public Article findById(UUID articleId, UUID requestId) {
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
