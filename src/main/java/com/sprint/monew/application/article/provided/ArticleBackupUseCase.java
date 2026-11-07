package com.sprint.monew.application.article.provided;

import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ArticleBackupUseCase {

    List<ArticleResult> readArticles(
            LocalDate date,
            UUID afterId,
            @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.") int limit);

    List<ArticleBackupResult> backupChunk(
            LocalDate date, long partNumber, List<ArticleResult> articles);
}
