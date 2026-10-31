package com.sprint.monew.adapter.out.article.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NaverNewsMapperTest {
    private static final String DATE = "Thu, 29 Oct 2026 16:00:00 +0900";
    private final NaverNewsMapper mapper = new NaverNewsMapper();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("원문 URL과 대체 URL이 모두 없으면 예외가 발생한다.")
    void givenMissingUrls_whenToCollectedArticle_thenThrowException(String url) {
        // given
        NaverNewsResponse.ArticleItem item =
                new NaverNewsResponse.ArticleItem("기사", url, url, "요약", DATE);

        // when
        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> mapper.toCollectedArticle(item));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(ArticleErrorType.ARTICLE_SOURCE_URL_MISSING);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "<b></b>"})
    @DisplayName("정제한 기사 제목이 비어 있으면 예외가 발생한다.")
    void givenEmptyTitle_whenToCollectedArticle_thenThrowException(String title) {
        // given
        NaverNewsResponse.ArticleItem item =
                new NaverNewsResponse.ArticleItem(title, "https://example.com", null, "요약", DATE);

        // when
        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> mapper.toCollectedArticle(item));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(ArticleErrorType.ARTICLE_TITLE_MISSING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-a-date", "2026-10-29"})
    @DisplayName("발행일 형식이 잘못되면 파싱 오류를 원인으로 보존한 예외가 발생한다.")
    void givenInvalidPublishedAt_whenToCollectedArticle_thenThrowExceptionWithCause(String date) {
        // given
        NaverNewsResponse.ArticleItem item =
                new NaverNewsResponse.ArticleItem("기사", "https://example.com", null, "요약", date);

        // when
        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> mapper.toCollectedArticle(item));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(ArticleErrorType.ARTICLE_PUBLISHED_AT_INVALID);
        assertThat(thrown).hasCauseInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("원문 URL과 요약이 없으면 대체 URL과 빈 요약으로 기사를 변환한다.")
    void givenFallbackUrlAndMissingDescription_whenToCollectedArticle_thenReturnArticle() {
        // given
        NaverNewsResponse.ArticleItem item =
                new NaverNewsResponse.ArticleItem(
                        "<b>기사</b>", null, "https://example.com", null, DATE);

        // when
        CollectedArticle article = mapper.toCollectedArticle(item);

        // then
        assertThat(article.sourceUrl()).isEqualTo("https://example.com");
        assertThat(article.title()).isEqualTo("기사");
        assertThat(article.summary()).isEmpty();
    }
}
