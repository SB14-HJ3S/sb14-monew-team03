package com.sprint.monew.application;

import com.sprint.monew.application.article.ArticleBackupService;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleBackupServiceImpl implements ArticleBackupService {

    private final ArticleRepositoryPort repository;

    private static final ZoneId BACKUP_ZONE = ZoneId.of("Asia/Seoul");

    @Override
    @Transactional(readOnly = true)
    public List<Article> getArticlesToBackup(LocalDate date) {
        Instant from = date.atStartOfDay(BACKUP_ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(BACKUP_ZONE).toInstant();

        return repository.getArticlesInDateRange(from, to);
    }
}
