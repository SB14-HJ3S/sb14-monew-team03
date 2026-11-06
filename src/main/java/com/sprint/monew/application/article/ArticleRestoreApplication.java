package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleRestoreUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleRestoreResult;
import com.sprint.monew.application.article.required.ArticleInterestRepositoryPort;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@ApplicationService
public class ArticleRestoreApplication implements ArticleRestoreUseCase {

    private static final int MAX_RESTORE_DAYS = 31;

    private final ArticleBackupStorageService articleBackupService;
    private final ArticleRepositoryPort articleService;
    private final InterestService interestService;
    private final ArticleInterestRepositoryPort articleInterestService;

    @Override
    @Transactional
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
            List<ArticleBackupItem> entries = new ArrayList<>();
            for (ArticleBackupItem item : articleBackupService.readAll(day)) {
                if (!item.publishedAt().isBefore(from) && !item.publishedAt().isAfter(end)) {
                    entries.add(item);
                }
            }
            List<UUID> ids = restoreDay(entries);
            results.add(new ArticleRestoreResult(day, ids, ids.size()));
        }
        return results;
    }

    private List<UUID> restoreDay(List<ArticleBackupItem> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        Set<UUID> interestIds = new HashSet<>();
        for (ArticleBackupItem entry : entries) {
            interestIds.addAll(entry.interestIds());
        }
        if (interestIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, Interest> byId = new HashMap<>();
        for (Interest interest : interestService.findAllById(interestIds)) {
            byId.put(interest.getId(), interest);
        }

        List<String> urls = entries.stream().map(ArticleBackupItem::sourceUrl).toList();
        Set<String> existingUrls = new HashSet<>();

        articleService
                .findAllBySourceUrlIn(urls)
                .forEach(article -> existingUrls.add(article.getSourceUrl()));

        List<Article> candidates = new ArrayList<>();
        Map<String, List<Interest>> interestsByUrl = new HashMap<>();
        for (ArticleBackupItem entry : entries) {
            List<Interest> remaining =
                    entry.interestIds().stream()
                            .distinct()
                            .filter(byId::containsKey)
                            .map(byId::get)
                            .toList();
            if (!remaining.isEmpty() && existingUrls.add(entry.sourceUrl())) {
                candidates.add(
                        Article.create(
                                entry.source(),
                                entry.sourceUrl(),
                                entry.title(),
                                entry.publishedAt(),
                                entry.summary()));
                interestsByUrl.put(entry.sourceUrl(), remaining);
            }
        }
        List<Article> saved = articleService.saveAll(candidates);
        List<ArticleInterest> links = new ArrayList<>();
        for (Article article : saved) {
            for (Interest interest : interestsByUrl.get(article.getSourceUrl())) {
                links.add(ArticleInterest.create(article, interest));
            }
        }
        articleInterestService.saveAll(links);
        for (Article a : saved) {
            log.info(
                    "기사 복구 완료. 제목={},  게시일시={},  id={}",
                    a.getTitle(),
                    a.getPublishedAt(),
                    a.getId());
        }
        return saved.stream().map(Article::getId).toList();
    }
}
