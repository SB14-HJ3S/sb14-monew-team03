package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import java.time.LocalDate;
import java.util.List;

public interface ArticleBackupStoragePort {

    ArticleBackupResult save(long partNumber, ArticleBackupFile backupFile);

    List<ArticleBackupItem> readAll(LocalDate date);
}
