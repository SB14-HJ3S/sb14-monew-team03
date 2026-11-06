package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import java.time.LocalDate;
import java.util.List;

public interface ArticleBackupStorageService {
    ArticleBackupResult save(long partNumber, ArticleBackupFile file);

    List<String> findKeys(LocalDate date);

    List<ArticleBackupItem> readFile(String key);

    List<ArticleBackupItem> readAll(LocalDate date);
}
