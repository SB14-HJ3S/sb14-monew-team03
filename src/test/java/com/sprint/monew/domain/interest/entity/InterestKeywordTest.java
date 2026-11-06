package com.sprint.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InterestKeywordTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("InterestKeyword 생성 시 keyword가 null, 빈 문자열, 공백이면 예외 발생")
    void givenBlankName_whenCreate_thenThrow(String keyword) {
        // given
        Interest interest = Interest.create("관심사", List.of("기존 키워드"));

        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class, () -> InterestKeyword.create(interest, keyword));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INVALID_INTEREST_KEYWORD);
        assertThat(thrown.getDetails()).containsEntry("keyword", keyword);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, InterestKeyword.MAX_KEYWORD_LENGTH})
    @DisplayName("유효한 길이의 키워드로 생성하면 관심사와 키워드를 저장한다.")
    void givenValidKeyword_whenCreate_thenRetainInterestAndKeyword(int length) {
        // given
        Interest interest = Interest.create("관심사", List.of("기존 키워드"));
        String keyword = "가".repeat(length);

        // when
        InterestKeyword created = InterestKeyword.create(interest, keyword);

        // then
        assertThat(created.getInterest()).isSameAs(interest);
        assertThat(created.getKeyword()).isEqualTo(keyword);
    }

    @Test
    @DisplayName("키워드가 최대 길이 255자를 초과하면 예외가 발생한다.")
    void givenTooLongKeyword_whenCreate_thenThrow() {
        // given
        Interest interest = Interest.create("관심사", List.of("기존 키워드"));
        String keyword = "가".repeat(InterestKeyword.MAX_KEYWORD_LENGTH + 1);

        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class, () -> InterestKeyword.create(interest, keyword));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INVALID_INTEREST_KEYWORD);
        assertThat(thrown.getDetails()).containsEntry("keyword", keyword);
    }
}
