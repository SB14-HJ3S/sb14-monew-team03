package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;

public interface ArticleBackupStoragePort {
    ArticleBackupResult save(String runId, long partNumber, ArticleBackupFile backupFile);
}
