package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import java.time.LocalDate;
import java.util.List;

public interface ArticleBackupStoragePort {

    ArticleBackupResult save(ArticleBackupFile backupFile);

    List<String> findKeys(LocalDate date);

    List<ArticleBackupItem> readFile(String key);

    List<ArticleBackupItem> readAll(LocalDate date);
}
