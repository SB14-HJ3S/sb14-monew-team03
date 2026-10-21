package com.sprint.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterestTest {

    @Test
    @DisplayName("Interest 생성 시 name이 비어있으면 예외 발생")
    void givenBlankName_whenCreate_thenThrow() {
        String blank = null;
        List<String> keywords = List.of("1", "2", "3");

        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class, () -> Interest.create(blank, keywords));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INTEREST_NAME_REQUIRED);
    }

    @Test
    @DisplayName("Interest 생성 시 keywords 비어있으면 예외 발생")
    void givenEmptyKeywords_whenCreate_thenThrow() {
        String name = "박수민";
        List<String> empty = Collections.emptyList();

        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class, () -> Interest.create(name, empty));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INTEREST_KEYWORD_REQUIRED);
    }
}
