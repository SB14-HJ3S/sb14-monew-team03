package com.sprint.monew.application.article.provided.usecase;

import com.sprint.monew.application.article.provided.usecase.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.usecase.dto.ArticleResult;
import com.sprint.monew.application.article.provided.usecase.dto.ArticleViewStatusResult;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ArticleSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public interface ArticleUseCase {
    PageResult<ArticleResult> findArticlePage(@NotNull @Valid ArticleQuery query);

    ArticleResult findById(@NotNull(message = "기사 ID는 필수입니다.") UUID articleId);

    List<ArticleSourceType> findSourceTypes();

    void softDeleteArticle(@NotNull UUID articleId);

    void hardDeleteArticle(@NotNull UUID articleId);

    ArticleViewStatusResult register(@NotNull(message = "기사 ID는 필수입니다.") UUID articleId);
}
