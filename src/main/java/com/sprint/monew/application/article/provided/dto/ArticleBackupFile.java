package com.sprint.monew.application.article.provided.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ArticleBackupFile(
        LocalDate backupDate, // 백업한 기사들이 생성된 날짜
        LocalDateTime backedUpAt, // 백업을 진행한 일시
        int articleCount,
        List<ArticleBackupItem> articles) {}
