package com.sprint.monew.application.article.provided.usecase;

import com.sprint.monew.application.article.provided.usecase.dto.ArticleRestoreResult;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRestoreUseCase {

    List<ArticleRestoreResult> restoreArticles(
            @NotNull(message = "복구 시작 일시는 필수입니다.") LocalDateTime from,
            @NotNull(message = "복구 종료 일시는 필수입니다.") LocalDateTime to);
}
