package com.sprint.monew.application.article.provided;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import java.time.LocalDate;

public interface ArticleBackupUseCase {
    ArticleBackupFile getArticlesToBackup(LocalDate date);
}
