package com.sprint.monew.adapter.out.article.rss;

import static org.assertj.core.api.Assertions.*;

import com.rometools.rome.feed.synd.*;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RssNewsMapperTest {
    private final RssNewsMapper mapper = new RssNewsMapper();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" "})
    @DisplayName("URL이 없으면 기사 URL 누락 예외를 발생시킨다.")
    void givenMissingUrl_whenMap_thenThrowMonewException(String url) {
        // given
        SyndEntry entry = entry();
        entry.setLink(url);
        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class,
                        () -> mapper.toCollectedArticle(ArticleSourceType.HANKYUNG, entry));
        // then
        assertThat(thrown.getErrorType()).isEqualTo(ArticleErrorType.ARTICLE_SOURCE_URL_MISSING);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" "})
    @DisplayName("제목이 없으면 기사 제목 누락 예외를 발생시킨다.")
    void givenMissingTitle_whenMap_thenThrowMonewException(String title) {
        // given
        SyndEntry entry = entry();
        entry.setTitle(title);
        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class,
                        () -> mapper.toCollectedArticle(ArticleSourceType.CHOSUN, entry));
        // then
        assertThat(thrown.getErrorType()).isEqualTo(ArticleErrorType.ARTICLE_TITLE_MISSING);
    }

    @Test
    @DisplayName("발행일이 없으면 발행일 오류를 발생시킨다.")
    void givenMissingDate_whenMap_thenThrowMonewException() {
        // given
        SyndEntry entry = entry();
        entry.setPublishedDate(null);
        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class,
                        () -> mapper.toCollectedArticle(ArticleSourceType.YONHAP, entry));
        // then
        assertThat(thrown.getErrorType()).isEqualTo(ArticleErrorType.ARTICLE_PUBLISHED_AT_INVALID);
    }

    @Test
    @DisplayName("서울 시간으로 변환하고 HTML·이미지·연합 캡션을 제거한다.")
    void givenHtmlSummary_whenMap_thenNormalizeDateAndSummary() {
        // given
        SyndEntry entry = entry();
        entry.setDescription(
                content(
                        "<div class='ynaimage'>캡션<img src='x'></div><p>AI &amp; 뉴스<img src='y'></p>"));
        // when
        var result = mapper.toCollectedArticle(ArticleSourceType.YONHAP, entry);
        // then
        assertThat(result.publishedAt()).isEqualTo(LocalDateTime.of(2026, 10, 30, 12, 0));
        assertThat(result.summary()).isEqualTo("AI & 뉴스");
        assertThat(result.sourceUrl()).isEqualTo("https://example.com/rss");
        assertThat(result.title()).isEqualTo("제목");
    }

    @Test
    @DisplayName("설명이 비어 있으면 contents에서 첫 번째 유효 요약을 선택한다.")
    void givenEmptyDescription_whenMap_thenUseContentFallback() {
        // given
        SyndEntry entry = entry();
        entry.setDescription(content("<img src='x'>"));
        entry.setContents(List.of(content(" "), content("<p>대체 요약</p>"), content("뒤의 요약")));
        // when
        var result = mapper.toCollectedArticle(ArticleSourceType.CHOSUN, entry);
        // then
        assertThat(result.summary()).isEqualTo("대체 요약");
    }

    private SyndEntry entry() {
        SyndEntry entry = new SyndEntryImpl();
        entry.setLink(" https://example.com/rss ");
        entry.setTitle(" 제목 ");
        entry.setPublishedDate(Date.from(Instant.parse("2026-10-30T03:00:00Z")));
        return entry;
    }

    private SyndContent content(String value) {
        SyndContent content = new SyndContentImpl();
        content.setType("text/html");
        content.setValue(value);
        return content;
    }
}
