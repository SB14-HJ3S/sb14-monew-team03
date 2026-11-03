package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleBackupApplicationTest {

    @Mock private ArticleService articleService;
    @Mock private ArticleInterestService articleInterestService;
    @Mock private ArticleBackupStorageService storageService;
    @InjectMocks private ArticleBackupApplication application;

    @Test
    @DisplayName("기사별 관심사를 구분하고 원래 생성 시각을 보존하여 백업한다")
    void backupChunk() {
        LocalDate date = LocalDate.of(2026, 11, 3);
        ArticleResult first = article();
        ArticleResult second = article();
        UUID firstInterest = UUID.randomUUID();
        UUID secondInterest = UUID.randomUUID();
        when(articleInterestService.findInterestIdsByArticleIds(List.of(first.id(), second.id())))
                .thenReturn(
                        Map.of(
                                first.id(),
                                List.of(firstInterest),
                                second.id(),
                                List.of(secondInterest)));
        ArticleBackupResult expected = new ArticleBackupResult("backup-key", date, 2);
        when(storageService.save(eq(1L), any(ArticleBackupFile.class))).thenReturn(expected);

        ArticleBackupResult result = application.backupChunk(date, 1L, List.of(first, second));

        ArgumentCaptor<ArticleBackupFile> captor = ArgumentCaptor.forClass(ArticleBackupFile.class);
        verify(storageService).save(eq(1L), captor.capture());
        ArticleBackupFile file = captor.getValue();
        assertThat(result).isEqualTo(expected);
        assertThat(file.backupDate()).isEqualTo(date);
        assertThat(file.count()).isEqualTo(2);
        assertThat(file.items()).hasSize(2);
        var item = file.items().get(0);
        assertThat(item.id()).isEqualTo(first.id());
        assertThat(item.source()).isEqualTo(first.source());
        assertThat(item.sourceUrl()).isEqualTo(first.sourceUrl());
        assertThat(item.title()).isEqualTo(first.title());
        assertThat(item.summary()).isEqualTo(first.summary());
        assertThat(item.publishedAt()).isEqualTo(first.publishedAt());
        assertThat(item.createdAt()).isEqualTo(first.createdAt());
        assertThat(item.interestIds()).containsExactly(firstInterest);
        assertThat(file.items().get(1).id()).isEqualTo(second.id());
        assertThat(file.items().get(1).interestIds()).containsExactly(secondInterest);
    }

    @Test
    @DisplayName("관심사 연결이 없는 기사도 빈 관심사 목록으로 저장한다")
    void backupWithoutInterests() {
        ArticleResult article = article();
        when(articleInterestService.findInterestIdsByArticleIds(List.of(article.id())))
                .thenReturn(Map.of());

        application.backupChunk(LocalDate.of(2026, 11, 3), 2L, List.of(article));

        ArgumentCaptor<ArticleBackupFile> captor = ArgumentCaptor.forClass(ArticleBackupFile.class);
        verify(storageService).save(eq(2L), captor.capture());
        assertThat(captor.getValue().items().get(0).interestIds()).isEmpty();
    }

    @Test
    @DisplayName("저장 실패는 호출자에게 그대로 전달한다")
    void propagateStorageFailure() {
        ArticleResult article = article();
        when(articleInterestService.findInterestIdsByArticleIds(List.of(article.id())))
                .thenReturn(Map.of());
        MonewException failure = new MonewException(ArticleErrorType.ARTICLE_BACKUP_UPLOAD_FAILED);
        when(storageService.save(eq(1L), any(ArticleBackupFile.class))).thenThrow(failure);

        assertThatThrownBy(
                        () ->
                                application.backupChunk(
                                        LocalDate.of(2026, 11, 3), 1L, List.of(article)))
                .isSameAs(failure);
    }

    private ArticleResult article() {
        UUID id = UUID.randomUUID();
        return new ArticleResult(
                id,
                ArticleSourceType.NAVER,
                "https://example.com/" + id,
                "제목",
                LocalDateTime.of(2026, 11, 3, 9, 0),
                Instant.parse("2026-11-03T01:00:00Z"),
                "요약",
                0,
                0,
                false);
    }
}
