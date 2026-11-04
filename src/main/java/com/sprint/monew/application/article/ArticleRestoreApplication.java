package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleRestoreUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleRestoreResult;
import com.sprint.monew.global.annotation.ApplicationService;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationService
public class ArticleRestoreApplication implements ArticleRestoreUseCase {

    @Override
    public List<ArticleRestoreResult> restoreArticles(LocalDateTime from, LocalDateTime to) {
        return List.of();
    }
}
