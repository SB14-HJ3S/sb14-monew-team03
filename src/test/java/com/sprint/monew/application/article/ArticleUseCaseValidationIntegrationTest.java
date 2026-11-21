package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.application.article.provided.usecase.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.usecase.ArticleRestoreUseCase;
import com.sprint.monew.application.article.provided.usecase.ArticleUseCase;
import com.sprint.monew.application.article.provided.usecase.dto.ArticleQuery;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.support.IntegrationTest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ArticleUseCaseValidationIntegrationTest {

    @Autowired private ArticleUseCase articleUseCase;
    @Autowired private ArticleBackupUseCase articleBackupUseCase;
    @Autowired private ArticleRestoreUseCase articleRestoreUseCase;

    @Test
    @DisplayName("목록 조회 개수가 0이면 한국어 검증 메시지를 반환한다")
    void givenZeroLimit_whenFindArticlePage_thenConstraintViolation() {
        ArticleQuery query =
                new ArticleQuery(
                        null,
                        null,
                        null,
                        null,
                        null,
                        ArticleQuery.SortOrder.PUBLISH_DATE,
                        SortDirection.DESC,
                        null,
                        null,
                        0);

        assertThatThrownBy(() -> articleUseCase.findArticlePage(query))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("조회 개수는 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("백업 조회 개수가 0이면 한국어 검증 메시지를 반환한다")
    void givenZeroLimit_whenReadArticles_thenConstraintViolation() {
        LocalDate date = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> articleBackupUseCase.readArticles(date, null, 0))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("조회 개수는 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("복구 시작 일시가 null이면 검증 예외가 발생한다")
    void givenNullFrom_whenRestoreArticles_thenConstraintViolation() {
        LocalDateTime to = LocalDateTime.of(2026, 11, 1, 23, 59);

        assertThatThrownBy(() -> articleRestoreUseCase.restoreArticles(null, to))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("복구 시작 일시는 필수입니다.");
    }

    @Test
    @DisplayName("복구 종료 일시가 null이면 검증 예외가 발생한다")
    void givenNullTo_whenRestoreArticles_thenConstraintViolation() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);

        assertThatThrownBy(() -> articleRestoreUseCase.restoreArticles(from, null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("복구 종료 일시는 필수입니다.");
    }

    @Test
    @DisplayName("기사 ID 없이 단건 조회하면 검증 예외가 발생한다")
    void givenNullArticleId_whenFindById_thenConstraintViolation() {
        assertThatThrownBy(() -> articleUseCase.findById(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("기사 ID는 필수입니다.");
    }

    @Test
    @DisplayName("기사 ID 없이 뷰 등록하면 검증 예외가 발생한다")
    void givenNullArticleId_whenRegister_thenConstraintViolation() {
        assertThatThrownBy(() -> articleUseCase.register(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("기사 ID는 필수입니다.");
    }
}
