package com.sprint.monew.application.article.provided;

import com.sprint.monew.application.article.provided.dto.ArticleRestoreResult;
import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRestoreUseCase {

    List<ArticleRestoreResult> restoreArticles(LocalDateTime from, LocalDateTime to);
}
