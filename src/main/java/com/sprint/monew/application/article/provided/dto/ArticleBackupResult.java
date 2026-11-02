package com.sprint.monew.application.article.provided.dto;

import java.time.LocalDate;

public record ArticleBackupResult(String key, LocalDate backupDate, int count) {

    public static ArticleBackupResult of(String key, ArticleBackupFile file) {
        return new ArticleBackupResult(key, file.backupDate(), file.count());
    }
}
