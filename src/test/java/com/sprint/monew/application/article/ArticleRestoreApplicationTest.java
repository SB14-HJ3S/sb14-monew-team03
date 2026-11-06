package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleRestoreResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleRestoreApplicationTest {

    @Mock private ArticleBackupStorageService storage;
    @Mock private ArticleRestoreService restoreService;
    @InjectMocks private ArticleRestoreApplication application;

    @Test
    @DisplayName("파일 하나를 읽고 저장한 후 다음 파일을 읽으며 날짜별 복구 결과를 합산한다")
    void restoresFilesSequentially() {
        // given
        LocalDate date = LocalDate.of(2026, 11, 3);
        ArticleBackupItem first = item(date);
        ArticleBackupItem second = item(date);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        when(storage.findKeys(date)).thenReturn(List.of("first.json.gz", "second.json.gz"));
        when(storage.readFile("first.json.gz")).thenReturn(List.of(first));
        when(storage.readFile("second.json.gz")).thenReturn(List.of(second));
        when(restoreService.restore(List.of(first))).thenReturn(List.of(firstId));
        when(restoreService.restore(List.of(second))).thenReturn(List.of(secondId));

        // when
        List<ArticleRestoreResult> results =
                application.restoreArticles(date.atStartOfDay(), date.atTime(23, 59, 59));

        // then
        assertThat(results)
                .containsExactly(new ArticleRestoreResult(date, List.of(firstId, secondId), 2));
        InOrder order = inOrder(storage, restoreService);
        order.verify(storage).findKeys(date);
        order.verify(storage).readFile("first.json.gz");
        order.verify(restoreService).restore(List.of(first));
        order.verify(storage).readFile("second.json.gz");
        order.verify(restoreService).restore(List.of(second));
        order.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("파일 읽기에 실패하면 DB 복구를 호출하지 않고 오류를 전달한다")
    void propagatesReadFailure() {
        // given
        LocalDate date = LocalDate.of(2026, 11, 3);
        IllegalStateException failure = new IllegalStateException("S3 조회 실패");
        when(storage.findKeys(date)).thenReturn(List.of("first.json.gz"));
        when(storage.readFile("first.json.gz")).thenThrow(failure);

        // when
        Throwable thrown =
                catchThrowable(
                        () ->
                                application.restoreArticles(
                                        date.atStartOfDay(), date.atTime(23, 59, 59)));

        // then
        assertThat(thrown).isSameAs(failure);
        verifyNoInteractions(restoreService);
    }

    private ArticleBackupItem item(LocalDate date) {
        return new ArticleBackupItem(
                UUID.randomUUID(),
                ArticleSourceType.NAVER,
                "https://example.com/" + UUID.randomUUID(),
                "기사",
                "요약",
                date.atTime(12, 0),
                Instant.parse("2026-11-03T03:00:00Z"),
                List.of());
    }
}
