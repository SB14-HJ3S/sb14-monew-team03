package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.annotation.ApplicationService;
import java.time.LocalDate;
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
    public List<Article> readArticles(LocalDate date, UUID afterId, int limit) {
        return articleService.getArticlesForBackup(date, afterId, limit);
    }

    @Override
    public ArticleBackupResult backupChunk(
            LocalDate date, String runId, long partNumber, List<Article> articles) {
        List<UUID> articleIds = articles.stream().map(Article::getId).toList();

        Map<UUID, List<UUID>> interestIdsByArticleId =
                articleInterestService.findInterestIdsByArticleIds(articleIds);

        List<ArticleBackupItem> items =
                articles.stream()
                        .map(
                                article ->
                                        ArticleBackupItem.from(
                                                article,
                                                interestIdsByArticleId.getOrDefault(
                                                        article.getId(), List.of())))
                        .toList();

        ArticleBackupFile file = ArticleBackupFile.of(date, items);

        return articleBackupStorageService.save(runId, partNumber, file);
    }
}
