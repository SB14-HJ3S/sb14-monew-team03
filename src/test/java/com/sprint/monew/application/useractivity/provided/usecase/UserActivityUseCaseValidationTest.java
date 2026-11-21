package com.sprint.monew.application.useractivity.provided.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.sprint.monew.support.IntegrationTest;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class UserActivityUseCaseValidationTest {

    @Autowired UserActivityUseCase userActivityUseCase;

    @Test
    void userId가_null이면_예외() {
        assertThatThrownBy(() -> userActivityUseCase.findByUserId(null))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void 닉네임이_공백이면_예외() {
        assertThatThrownBy(() -> userActivityUseCase.nicknameUpdated(UUID.randomUUID(), " "))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void 좋아요수가_음수면_예외() {
        assertThatThrownBy(
                        () ->
                                userActivityUseCase.commentUnliked(
                                        UUID.randomUUID(), UUID.randomUUID(), -1))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
