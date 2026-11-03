package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;

public interface ArticleBackupStorageService {
    ArticleBackupResult save(long partNumber, ArticleBackupFile file);
}
