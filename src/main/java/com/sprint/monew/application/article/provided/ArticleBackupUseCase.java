package com.sprint.monew.application.article.provided;

import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ArticleBackupUseCase {

    List<ArticleResult> readArticles(LocalDate date, UUID afterId, int limit);

    List<ArticleBackupResult> backupChunk(
            LocalDate date, long partNumber, List<ArticleResult> articles);
}
