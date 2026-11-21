package com.sprint.monew.application.article.provided.service;

import com.sprint.monew.application.article.provided.service.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.service.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.usecase.dto.ArticleBackupResult;
import java.time.LocalDate;
import java.util.List;

public interface ArticleBackupStorageService {
    ArticleBackupResult save(ArticleBackupFile file);

    List<String> findKeys(LocalDate date);

    List<ArticleBackupItem> readFile(String key);

    List<ArticleBackupItem> readAll(LocalDate date);
}
