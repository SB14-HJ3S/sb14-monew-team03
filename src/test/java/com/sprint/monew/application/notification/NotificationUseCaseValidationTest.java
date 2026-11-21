package com.sprint.monew.application.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.application.notification.provided.usecase.NotificationUseCase;
import com.sprint.monew.application.notification.provided.usecase.dto.NotificationQuery;
import com.sprint.monew.support.IntegrationTest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class NotificationUseCaseValidationTest {

    @Autowired private NotificationUseCase notificationUseCase;

    @Test
    @DisplayName("limit이 1보다 작은 조회 조건이면 서비스 실행 전에 검증 예외가 난다.")
    void givenZeroLimit_whenFindByQuery_thenConstraintViolation() {
        NotificationQuery query = NotificationQuery.of(null, null, 0);

        assertThatThrownBy(() -> notificationUseCase.findByQuery(query))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("조회 조건이 null이면 검증 예외가 난다.")
    void givenNullQuery_whenFindByQuery_thenConstraintViolation() {
        assertThatThrownBy(() -> notificationUseCase.findByQuery(null))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
