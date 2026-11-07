package com.sprint.monew.application.comment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.support.IntegrationTest;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// 프록시를 거쳐야 UseCase 검증이 도니까 스프링 빈으로 호출함
@IntegrationTest
class CommentUseCaseValidationIntegrationTest {

    private static final String COMMENT_ID_REQUIRED = "댓글 ID는 필수입니다.";

    @Autowired private CommentUseCase commentUseCase;

    private final UUID id = UUID.randomUUID();

    @Test
    @DisplayName("조회 조건이 null이면 검증 예외가 난다")
    void givenNullQuery_whenFindByQuery_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.findByQuery(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("조회 조건은 필수입니다.");
    }

    @Test
    @DisplayName("조회 개수가 0이면 한국어 검증 메시지가 나온다")
    void givenZeroLimit_whenFindByQuery_thenConstraintViolation() {
        CommentQuery query =
                CommentQuery.of(
                        id, CommentQuery.SortOrder.CREATED_AT, SortDirection.DESC, null, null, 0);

        assertThatThrownBy(() -> commentUseCase.findByQuery(query))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("조회 개수는 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("정렬 기준이 null이면 한국어 검증 메시지가 나온다")
    void givenNullOrderBy_whenFindByQuery_thenConstraintViolation() {
        CommentQuery query = CommentQuery.of(id, null, SortDirection.DESC, null, null, 10);

        assertThatThrownBy(() -> commentUseCase.findByQuery(query))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("정렬 기준은 필수입니다.");
    }

    @Test
    @DisplayName("정렬 방향이 null이면 한국어 검증 메시지가 나온다")
    void givenNullDirection_whenFindByQuery_thenConstraintViolation() {
        CommentQuery query =
                CommentQuery.of(id, CommentQuery.SortOrder.CREATED_AT, null, null, null, 10);

        assertThatThrownBy(() -> commentUseCase.findByQuery(query))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("정렬 방향은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 ID 없이 등록하면 검증 예외가 난다")
    void givenNullUserId_whenRegister_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.register(null, id, "댓글"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("사용자 ID는 필수입니다.");
    }

    @Test
    @DisplayName("기사 ID 없이 등록하면 검증 예외가 난다")
    void givenNullArticleId_whenRegister_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.register(id, null, "댓글"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("기사 ID는 필수입니다.");
    }

    @Test
    @DisplayName("내용이 공백이면 등록할 때 검증 예외가 난다")
    void givenBlankContent_whenRegister_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.register(id, id, "   "))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("댓글 내용은 필수입니다.");
    }

    @Test
    @DisplayName("내용이 공백이면 수정할 때 검증 예외가 난다")
    void givenBlankContent_whenChangeContent_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.changeContent(id, "   "))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("댓글 내용은 필수입니다.");
    }

    @Test
    @DisplayName("댓글 ID 없이 수정하면 검증 예외가 난다")
    void givenNullCommentId_whenChangeContent_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.changeContent(null, "수정"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining(COMMENT_ID_REQUIRED);
    }

    @Test
    @DisplayName("댓글 ID 없이 단건 조회하면 검증 예외가 난다")
    void givenNullCommentId_whenFindById_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.findById(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining(COMMENT_ID_REQUIRED);
    }

    @Test
    @DisplayName("댓글 ID 없이 좋아요를 누르면 검증 예외가 난다")
    void givenNullCommentId_whenLike_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.like(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining(COMMENT_ID_REQUIRED);
    }

    @Test
    @DisplayName("댓글 ID 없이 좋아요를 취소하면 검증 예외가 난다")
    void givenNullCommentId_whenUnlike_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.unlike(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining(COMMENT_ID_REQUIRED);
    }

    @Test
    @DisplayName("댓글 ID 없이 논리 삭제하면 검증 예외가 난다")
    void givenNullCommentId_whenSoftDelete_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.softDeleteComment(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining(COMMENT_ID_REQUIRED);
    }

    @Test
    @DisplayName("댓글 ID 없이 물리 삭제하면 검증 예외가 난다")
    void givenNullCommentId_whenHardDelete_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.hardDeleteComment(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining(COMMENT_ID_REQUIRED);
    }

    @Test
    @DisplayName("기사 ID 없이 좋아요 알림을 정리하면 검증 예외가 난다")
    void givenNullArticleId_whenDeleteLikeNotifications_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.deleteLikeNotificationsByArticle(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("기사 ID는 필수입니다.");
    }

    @Test
    @DisplayName("사용자 ID 목록 없이 숫자를 정리하면 검증 예외가 난다")
    void givenNullUserIds_whenCleanupCounts_thenConstraintViolation() {
        assertThatThrownBy(() -> commentUseCase.cleanupCountsBeforeUserHardDeletion(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("사용자 ID 목록은 필수입니다.");
    }
}
