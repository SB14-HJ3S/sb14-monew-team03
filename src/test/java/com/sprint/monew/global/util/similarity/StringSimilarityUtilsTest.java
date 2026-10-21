package com.sprint.monew.global.util.similarity;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class StringSimilarityUtilsTest {

    @Test
    @DisplayName("완전히 같은 문자열의 유사도는 1")
    void givenSameStrings_whenCalculateSimilarity_thenSimilarityEquals1() {
        String s1 = "문자열1";
        String s2 = "문자열1";

        double result = StringSimilarityUtils.calculateSimilarity(s1, s2);

        Assertions.assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("문자열 유사도 계산 시 공백의 제외")
    void givenSameStringsExceptWhitespace_whenCalculateSimilarity_thenSimilarityEquals1() {
        String s1 = "문 자 열 1";
        String s2 = "문자열1";

        double result = StringSimilarityUtils.calculateSimilarity(s1, s2);

        Assertions.assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("문자열이 하나가 empty또는 null인 경우, 유사도는 0")
    void givenNullString_whenCalculateSimilarity_thenSimilarityEquals0() {
        String nullStr = null;
        String str = "str";

        double result = StringSimilarityUtils.calculateSimilarity(nullStr, str);

        Assertions.assertThat(result).isZero();
    }

    @Test
    @DisplayName("문자열 둘 다 empty 또는 null인 경우 유사도 계산 불가")
    void givenEmptyString_whenCalculateSimilarity_thenSimilarityEquals0() {
        String emptyStr = "";
        String nullStr = null;

        Assertions.assertThatThrownBy(
                        () -> StringSimilarityUtils.calculateSimilarity(emptyStr, nullStr))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("문자열들을 NFD 정규화한 후 Levenshtein 유사도를 계산하므로, 초성,중성,종성 단위로 삭제/삽입/치환 횟수만 고려하면 됨")
    void whenCalculateSimilarity_thenSimilarityReflectsJamoEditDistance() {
        String str1 = "박수민";
        String str2 = "박쑤믜";

        double result = StringSimilarityUtils.calculateSimilarity(str1, str2);

        // 2번째 음절의 초성(치환), 3번째 음절의 중성(치환), 3번째 음절의 종성(삭제) 3개가 다르므로 편집거리는 3
        final double expected = 1 - 3.0 / 8;
        Assertions.assertThat(result).isEqualTo(expected);
    }
}
