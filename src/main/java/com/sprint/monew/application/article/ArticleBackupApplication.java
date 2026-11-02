package com.sprint.monew.application.article;

import static com.sprint.monew.application.article.ArticleServiceImpl.BACKUP_ZONE;

import com.sprint.monew.application.article.provided.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.annotation.ApplicationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@ApplicationService
@RequiredArgsConstructor
public class ArticleBackupApplication implements ArticleBackupUseCase {

    private final ArticleService articleService;
    private final ArticleInterestService articleInterestService;

    @Override
    public ArticleBackupFile getArticlesToBackup(LocalDate date) { // 나중에 이 파라미터 없애기
        List<Article> articleList = articleService.getArticlesByDate(date);

        List<UUID> articleIdList = articleList.stream().map(Article::getId).toList();

        Map<UUID, List<UUID>> interestsByArticleId =
                articleInterestService.findInterestIdsByArticleIds(articleIdList);

        List<ArticleBackupItem> items =
                articleList.stream()
                        .map(
                                article ->
                                        ArticleBackupItem.from(
                                                article,
                                                interestsByArticleId.getOrDefault(
                                                        article.getId(), List.of())))
                        .toList();

        return new ArticleBackupFile(date, LocalDateTime.now(BACKUP_ZONE), items.size(), items);
    }
}
