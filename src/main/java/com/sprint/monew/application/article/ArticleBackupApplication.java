package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.annotation.ApplicationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@ApplicationService
@RequiredArgsConstructor
public class ArticleBackupApplication implements ArticleBackupUseCase {

    private final ArticleService articleService;
    private final ArticleInterestService articleInterestService;
    private final ArticleBackupStorageService articleBackupStorageService;

    @Override
    public List<ArticleResult> readArticles(LocalDate date, UUID afterId, int limit) {
        return articleService.getArticlesForBackup(date, afterId, limit).stream()
                .map(article -> ArticleResult.from(article, false))
                .toList();
    }

    @Override
    public List<ArticleBackupResult> backupChunk(
            LocalDate date, long partNumber, List<ArticleResult> articles) {
        List<UUID> articleIds = articles.stream().map(ArticleResult::id).toList();

        // 모든 기사ID에 대해 각 기사에 연결된 관심사ID들을 List로 받아온다
        // {기사ID : [관심사1ID, 관심사2ID...]}
        Map<UUID, List<UUID>> interestIdsByArticleId =
                articleInterestService.findInterestIdsByArticleIds(articleIds);

        List<ArticleBackupItem> items =
                articles.stream()
                        .map(
                                article ->
                                        ArticleBackupItem.from(
                                                article,
                                                interestIdsByArticleId.getOrDefault(
                                                        article.id(), List.of())))
                        .toList();

        Map<LocalDate, List<ArticleBackupItem>> itemsByDate = new LinkedHashMap<>();
        for (ArticleBackupItem item : items) {
            LocalDate publishedDate = item.publishedAt().toLocalDate();
            List<ArticleBackupItem> group = itemsByDate.get(publishedDate);
            if (group == null) {
                group = new ArrayList<>();
                itemsByDate.put(publishedDate, group);
            }
            group.add(item);
        }

        List<ArticleBackupResult> results = new ArrayList<>();
        for (Map.Entry<LocalDate, List<ArticleBackupItem>> entry : itemsByDate.entrySet()) {
            ArticleBackupFile file = ArticleBackupFile.of(entry.getKey(), entry.getValue());
            results.add(articleBackupStorageService.save(file));
        }
        return results;
    }
}
