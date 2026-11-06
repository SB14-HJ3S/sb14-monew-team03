package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.adapter.out.article.ArticleJpaRepository;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleRestoreResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleRestoreIntegrationTest {

    @Autowired private ArticleRestoreApplication application;
    @Autowired private ArticleService articleService;
    @Autowired private ArticleJpaRepository articles;
    @Autowired private EntityManager em;
    @MockitoBean private ArticleBackupStorageService storage;

    @Test
    @DisplayName("과거 발행 기사를 복구하면 발행일은 유지하고 생성일 기준 백업 대상으로 조회된다")
    void restoredArticleIsSelectedByCreationDate() {
        // given
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate publishedDate = today.minusDays(3);
        Fixture fixture = backupArticle(publishedDate);
        deleteArticle(fixture);

        // when
        List<ArticleRestoreResult> results = restore(publishedDate);
        em.flush();
        em.clear();

        // then
        Article restored = articles.findBySourceUrl(fixture.backup().sourceUrl()).orElseThrow();
        assertThat(results.get(0).restoredArticleIds()).containsExactly(restored.getId());
        assertThat(results.get(0).restoredArticleCount()).isEqualTo(1);
        assertThat(restored.getId()).isNotEqualTo(fixture.backup().id());
        assertThat(restored.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate())
                .isEqualTo(today);
        assertThat(restored.getPublishedAt()).isEqualTo(fixture.backup().publishedAt());
        assertThat(articleService.getArticlesForBackup(today, null, 100))
                .extracting(Article::getId)
                .containsExactly(restored.getId());
        assertThat(articleService.getArticlesForBackup(publishedDate, null, 100)).isEmpty();
        assertThat(
                        em.createQuery(
                                        "select ai.interest.id from ArticleInterest ai"
                                                + " where ai.article.id = :id",
                                        UUID.class)
                                .setParameter("id", restored.getId())
                                .getResultList())
                .containsExactly(fixture.interest().getId());
    }

    @Test
    @DisplayName("물리 삭제한 기사의 연결 관심사가 모두 삭제되면 복구하지 않아 백업 대상에도 포함되지 않는다")
    void deletedInterestPreventsRestoreAndBackup() {
        // given
        LocalDate date = LocalDate.of(2026, 11, 3);
        Fixture fixture = backupArticle(date);
        deleteArticle(fixture);
        em.remove(fixture.interest());
        em.flush();
        em.clear();

        // when
        List<ArticleRestoreResult> results = restore(date);
        em.flush();

        // then
        assertThat(results.get(0).restoredArticleCount()).isZero();
        assertThat(results.get(0).restoredArticleIds()).isEmpty();
        assertThat(articles.findBySourceUrl(fixture.backup().sourceUrl())).isEmpty();
        assertThat(articleService.getArticlesForBackup(date, null, 100)).isEmpty();
    }

    @Test
    @DisplayName("논리 삭제한 기사는 복구하지 않고 기존 ID와 삭제 상태를 유지한다")
    void softDeletedArticleStaysDeleted() {
        // given
        LocalDate date = LocalDate.of(2026, 11, 3);
        Fixture fixture = backupArticle(date);
        fixture.article().softDelete();
        em.flush();
        em.clear();
        var deletedAt = articles.findById(fixture.backup().id()).orElseThrow().getDeletedAt();
        em.clear();

        // when
        List<ArticleRestoreResult> results = restore(date);
        em.flush();
        em.clear();

        // then
        assertThat(results.get(0).restoredArticleCount()).isZero();
        assertThat(results.get(0).restoredArticleIds()).isEmpty();
        List<Article> matching =
                articles.findAllBySourceUrlIn(List.of(fixture.backup().sourceUrl()));
        assertThat(matching).hasSize(1);
        assertThat(matching.get(0).getId()).isEqualTo(fixture.backup().id());
        assertThat(matching.get(0).getDeletedAt()).isEqualTo(deletedAt);
    }

    private Fixture backupArticle(LocalDate date) {
        Interest interest = Interest.create("경제", List.of("경제"));
        Article article =
                Article.create(
                        ArticleSourceType.NAVER,
                        "https://example.com/" + UUID.randomUUID(),
                        "경제 기사",
                        date.atTime(12, 0),
                        "기사 요약");
        em.persist(interest);
        em.persist(article);
        ArticleInterest link = ArticleInterest.create(article, interest);
        em.persist(link);
        em.flush();
        ArticleBackupItem backup =
                new ArticleBackupItem(
                        article.getId(),
                        article.getSource(),
                        article.getSourceUrl(),
                        article.getTitle(),
                        article.getSummary(),
                        article.getPublishedAt(),
                        article.getCreatedAt(),
                        List.of(interest.getId()));
        String key = "articles/" + date + "/articles-" + date + "-000001.json.gz";
        when(storage.findKeys(date)).thenReturn(List.of(key));
        when(storage.readFile(key)).thenReturn(List.of(backup));
        return new Fixture(article, interest, link, backup);
    }

    private void deleteArticle(Fixture fixture) {
        em.remove(fixture.link());
        em.remove(fixture.article());
        em.flush();
    }

    private List<ArticleRestoreResult> restore(LocalDate date) {
        List<ArticleRestoreResult> results =
                application.restoreArticles(date.atStartOfDay(), date.atTime(23, 59, 59));
        String key = "articles/" + date + "/articles-" + date + "-000001.json.gz";
        verify(storage).readFile(key);
        return results;
    }

    private record Fixture(
            Article article, Interest interest, ArticleInterest link, ArticleBackupItem backup) {}
}
