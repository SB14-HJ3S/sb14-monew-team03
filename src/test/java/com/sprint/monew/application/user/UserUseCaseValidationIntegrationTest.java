package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.application.user.provided.usecase.UserUseCase;
import com.sprint.monew.support.IntegrationTest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class UserUseCaseValidationIntegrationTest {

    @Autowired private UserUseCase userUseCase;

    @Test
    @DisplayName("사용자 ID 없이 탈퇴를 부르면 메서드 실행 전에 userId 검증 오류가 난다")
    void givenNullUserId_whenSoftDeleteUser_thenConstraintViolation() {
        assertThatThrownBy(() -> userUseCase.softDeleteUser(null))
                .isInstanceOfSatisfying(
                        ConstraintViolationException.class,
                        e ->
                                assertThat(e.getConstraintViolations())
                                        .extracting(v -> v.getPropertyPath().toString())
                                        .containsExactly("softDeleteUser.userId"));
    }

    @Test
    @DisplayName("공백뿐인 닉네임으로 수정을 부르면 메서드 실행 전에 nickname 검증 오류가 난다")
    void givenBlankNickname_whenUpdateNickname_thenConstraintViolation() {
        assertThatThrownBy(() -> userUseCase.updateNickname(UUID.randomUUID(), "   "))
                .isInstanceOfSatisfying(
                        ConstraintViolationException.class,
                        e ->
                                assertThat(e.getConstraintViolations())
                                        .extracting(ConstraintViolation::getPropertyPath)
                                        .extracting(Object::toString)
                                        .containsExactly("updateNickname.nickname"));
    }
}
