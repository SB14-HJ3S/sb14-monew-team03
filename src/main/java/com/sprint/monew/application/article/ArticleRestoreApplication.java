package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleRestoreUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleRestoreResult;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@ApplicationService
public class ArticleRestoreApplication implements ArticleRestoreUseCase {

    private static final int MAX_RESTORE_DAYS = 31;

    private final ArticleBackupStorageService articleBackupService;
    private final ArticleRestoreService articleRestoreService;

    @Override
    public List<ArticleRestoreResult> restoreArticles(LocalDateTime from, LocalDateTime to) {
        if (Objects.isNull(from) || (Objects.isNull(to))) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST);
        }
        if (from.isAfter(to)) {
            throw new MonewException(
                    CommonErrorType.INVALID_REQUEST, Map.of("from", from, "to", to));
        }

        LocalDateTime end = to;
        long days = ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1;
        if (days > MAX_RESTORE_DAYS) {
            log.warn(
                    "기사 복구 요청 기간 초과: from={}, to={}, 요청 일수={}, 최대 일수={}",
                    from,
                    to,
                    days,
                    MAX_RESTORE_DAYS);
            throw new MonewException(
                    CommonErrorType.INVALID_REQUEST,
                    Map.of("maxDays", MAX_RESTORE_DAYS, "requestedDays", days));
        }

        List<ArticleRestoreResult> results = new ArrayList<>();
        for (LocalDate day = from.toLocalDate();
                !day.isAfter(to.toLocalDate());
                day = day.plusDays(1)) {
            List<UUID> ids = new ArrayList<>();
            for (String key : articleBackupService.findKeys(day)) {
                List<ArticleBackupItem> items = articleBackupService.readFile(key);
                List<ArticleBackupItem> entries = new ArrayList<>();
                for (ArticleBackupItem item : items) {
                    if (!item.publishedAt().isBefore(from) && !item.publishedAt().isAfter(end)) {
                        entries.add(item);
                    }
                }
                ids.addAll(articleRestoreService.restore(entries));
            }
            results.add(new ArticleRestoreResult(day, ids, ids.size()));
        }
        return results;
    }
}
