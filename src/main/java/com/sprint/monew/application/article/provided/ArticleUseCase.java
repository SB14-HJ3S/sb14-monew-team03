package com.sprint.monew.application.article.provided;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ArticleSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ArticleUseCase {
    PageResult<ArticleResult> findArticlePage(@NotNull @Valid ArticleQuery query);

    ArticleResult findById(UUID articleId);

    List<ArticleSourceType> findSourceTypes();

    void softDeleteArticle(UUID articleId);

    void hardDeleteArticle(UUID articleId);

    List<Article> restoreArticles(LocalDateTime from, LocalDateTime to);

    ArticleViewStatus register(UUID articleId, UUID requestId);

    void saveAndLink(CollectedArticle collectedArticle, Interest interest);
}
