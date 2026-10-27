package com.sprint.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterestTest {

    @Test
    @DisplayName("Interest 생성 시 name이 null,empty,only whitespace인 경우 예외 발생")
    void givenBlankName_whenCreate_thenThrow() {
        String whitespace = " ";
        List<String> keywords = List.of("1", "2", "3");

        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class, () -> Interest.create(whitespace, keywords));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INVALID_INTEREST_NAME);
    }

    @Test
    @DisplayName("Interest 생성 시 keywords 비어있으면 예외 발생")
    void givenEmptyKeywords_whenCreate_thenThrow() {
        String name = "박수민";
        List<String> empty = Collections.emptyList();

        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class, () -> Interest.create(name, empty));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INVALID_INTEREST_KEYWORD);
    }

    @Test
    @DisplayName("키워드 수정 시 삭제된 키워드를 제거하고 새로운 키워드를 추가한다")
    void givenNewKeywords_whenUpdateKeywords_thenReflectChanges() {
        Interest interest = Interest.create("관심사", List.of("기존", "유지"));
        InterestKeyword retainedKeyword = interest.getKeywords().get(1);

        Interest updated = interest.updateKeywords(List.of("유지", "추가"));

        assertThat(updated).isSameAs(interest);
        assertThat(updated.getKeywords())
                .extracting(InterestKeyword::getKeyword)
                .containsExactly("유지", "추가");
    }

    @Test
    @DisplayName("키워드 수정 시 빈 목록이면 예외가 발생한다")
    void givenEmptyKeywords_whenUpdateKeywords_thenThrow() {
        Interest interest = Interest.create("관심사", List.of("기존"));

        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class,
                        () -> interest.updateKeywords(Collections.emptyList()));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INVALID_INTEREST_KEYWORD);
        assertThat(interest.getKeywords())
                .extracting(InterestKeyword::getKeyword)
                .containsExactly("기존");
    }

    @Test
    @DisplayName("키워드 수정 시 최대 개수를 초과하면 예외가 발생한다")
    void givenTooManyKeywords_whenUpdateKeywords_thenThrow() {
        Interest interest = Interest.create("관심사", List.of("기존"));
        List<String> tooManyKeywords =
                IntStream.rangeClosed(1, Interest.MAX_KEYWORDS_SIZE + 1)
                        .mapToObj(String::valueOf)
                        .toList();

        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class, () -> interest.updateKeywords(tooManyKeywords));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INVALID_INTEREST_KEYWORD);
    }

    @Test
    @DisplayName("관심사의 키워드 이름을 등록 순서대로 반환한다")
    void givenInterest_whenGetKeywordNames_thenReturnKeywordNamesInOrder() {
        // given
        Interest interest = Interest.create("관심사", List.of("첫 번째", "두 번째"));

        // when
        List<String> keywordNames = interest.getKeywordNames();

        // then
        assertThat(keywordNames).containsExactly("첫 번째", "두 번째");
    }
}
