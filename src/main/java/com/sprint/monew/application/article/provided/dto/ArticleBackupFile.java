package com.sprint.monew.application.article.provided.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public record ArticleBackupFile(
        LocalDate backupDate, LocalDateTime createdAt, int count, List<ArticleBackupItem> items) {

    private static final ZoneId BACKUP_ZONE = ZoneId.of("Asia/Seoul");

    public static ArticleBackupFile of(LocalDate backupDate, List<ArticleBackupItem> items) {
        return new ArticleBackupFile(
                backupDate, LocalDateTime.now(BACKUP_ZONE), items.size(), items);
    }
}
