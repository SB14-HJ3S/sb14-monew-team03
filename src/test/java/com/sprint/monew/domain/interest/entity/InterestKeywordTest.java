package com.sprint.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterestKeywordTest {
    @Test
    @DisplayName("InterestKeyword 생성 시 keyword가 비어있으면 예외 발생")
    void givenBlankName_whenCreate_thenThrow() {
        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class, () -> InterestKeyword.create(null, ""));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INTEREST_KEYWORD_REQUIRED);
    }
}
