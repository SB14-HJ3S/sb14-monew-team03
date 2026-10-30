package com.sprint.monew.adapter.out.article.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class NaverNewsAdapterTest {
    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.Builder builder;

    private MockRestServiceServer server;
    private NaverNewsAdapter adapter;
    private static final String DATE = "Thu, 29 Oct 2026 16:00:00 +0900";
    private final Logger logger = (Logger) LoggerFactory.getLogger(NaverNewsAdapter.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void setUp() {
        logs.start();
        logger.addAppender(logs);
        RestClient.Builder httpBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(httpBuilder).build();
        // 어댑터의 실제 네트워크용 requestFactory 설정 대신 테스트 HTTP 클라이언트를 주입한다.
        when(builder.clone()).thenReturn(builder);
        when(builder.build()).thenReturn(httpBuilder.build());
        adapter =
                new NaverNewsAdapter(
                        builder,
                        new NaverNewsMapper(),
                        "https://example.com/news",
                        "test-id",
                        "test-secret");
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 429, 500})
    @DisplayName("외부 API가 오류 상태를 반환하면 원인을 보존한 예외가 발생한다.")
    void givenHttpError_whenSearch_thenThrowMonewExceptionWithCause(int status) {
        // given
        server.expect(
                        requestTo(
                                "https://example.com/news?query=AI&display=100&start=1&sort=date&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        // when
        MonewException failure =
                catchThrowableOfType(MonewException.class, () -> adapter.search("AI"));

        // then
        assertThat(failure.getErrorType()).isEqualTo(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        assertThat(failure).hasCauseInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    @DisplayName("네트워크 요청이 실패하면 최초 원인을 보존한 예외가 발생한다.")
    void givenNetworkFailure_whenSearch_thenPreserveRootCause() {
        // given
        IOException cause = new IOException("connection timed out");
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://example.com/news?")))
                .andRespond(withException(cause));

        // when
        MonewException failure =
                catchThrowableOfType(MonewException.class, () -> adapter.search("AI"));

        // then
        assertThat(failure.getErrorType()).isEqualTo(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        assertThat(failure).hasRootCause(cause);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"items\":null}"})
    @DisplayName("응답 본문이나 기사 목록이 누락되면 예외가 발생한다.")
    void givenMissingResponseOrItems_whenSearch_thenThrowException(String body) {
        // given
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://example.com/news?")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // when
        MonewException failure =
                catchThrowableOfType(MonewException.class, () -> adapter.search("AI"));

        // then
        assertThat(failure.getErrorType()).isEqualTo(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        server.verify();
    }

    @Test
    @DisplayName("검색 결과가 비어 있으면 추가 요청 없이 빈 목록을 반환한다.")
    void givenEmptyItems_whenSearch_thenReturnEmptyList() {
        // given
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://example.com/news?")))
                .andRespond(withSuccess("{\"total\":0,\"items\":[]}", MediaType.APPLICATION_JSON));

        // when
        List<CollectedArticle> result = adapter.search("AI");

        // then
        assertThat(result).isEmpty();

        server.verify();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logs.stop();
    }

    static Stream<Arguments> invalidItems() {
        return Stream.of(
                Arguments.of(
                        new NaverNewsResponse.ArticleItem("기사", null, null, "요약", DATE),
                        "ARTICLE_SOURCE_URL_MISSING"),
                Arguments.of(
                        new NaverNewsResponse.ArticleItem(
                                " ", "https://example.com/bad", null, "요약", DATE),
                        "ARTICLE_TITLE_MISSING"),
                Arguments.of(
                        new NaverNewsResponse.ArticleItem(
                                "기사", "https://example.com/bad", null, "요약", "invalid-date"),
                        "ARTICLE_PUBLISHED_AT_INVALID"),
                Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("invalidItems")
    @DisplayName("잘못된 기사만 건너뛰고 앞뒤 정상 기사를 보존하며 경고 로그를 남긴다.")
    void givenInvalidItemBetweenValidItems_whenSearch_thenKeepValidItemsAndWarn(
            NaverNewsResponse.ArticleItem invalid, String errorType) throws Exception {
        // given
        expectPage(1, 3, Arrays.asList(validItem(1), invalid, validItem(2)));

        // when
        List<CollectedArticle> result = adapter.search("AI");

        // then
        assertThat(result)
                .extracting(CollectedArticle::sourceUrl)
                .containsExactly("https://example.com/1", "https://example.com/2");
        assertThat(logs.list).hasSize(1);
        ILoggingEvent event = logs.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("keyword=AI", "start=1");
        if (errorType != null) {
            assertThat(event.getFormattedMessage()).contains(errorType);
            assertThat(event.getThrowableProxy().getClassName())
                    .isEqualTo(MonewException.class.getName());
        }
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"1, 101", "100, 101", "100, 100"})
    @DisplayName("변환 성공 개수와 관계없이 원본 페이지 크기와 전체 건수로 다음 페이지를 판단한다.")
    void givenInvalidItemsInFullPage_whenSearch_thenUseOriginalPagination(
            int invalidCount, int total) throws Exception {
        // given
        List<NaverNewsResponse.ArticleItem> firstPage = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            firstPage.add(i < invalidCount ? null : validItem(i));
        }
        expectPage(1, total, firstPage);
        if (total > 100) {
            expectPage(101, total, List.of(validItem(101)));
        }

        // when
        List<CollectedArticle> result = adapter.search("AI");

        // then
        List<String> expectedUrls = new ArrayList<>();
        for (int i = invalidCount; i < 100; i++) {
            expectedUrls.add("https://example.com/" + i);
        }
        if (total > 100) {
            expectedUrls.add("https://example.com/101");
        }
        assertThat(result)
                .extracting(CollectedArticle::sourceUrl)
                .containsExactlyElementsOf(expectedUrls);
        assertThat(logs.list).hasSize(invalidCount);
        server.verify();
    }

    @Test
    @DisplayName("마지막 페이지의 기사가 모두 잘못되어도 추가 요청 없이 빈 결과를 반환한다.")
    void givenOnlyInvalidItemsInLastPage_whenSearch_thenReturnEmptyList() throws Exception {
        // given
        expectPage(1, 1, Arrays.asList((NaverNewsResponse.ArticleItem) null));

        // when
        List<CollectedArticle> result = adapter.search("AI");

        // then
        assertThat(result).isEmpty();
        assertThat(logs.list).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("다음 페이지의 API 요청 실패는 기사 변환 오류처럼 무시하지 않고 전달한다.")
    void givenLaterPageHttpFailure_whenSearch_thenThrowException() throws Exception {
        // given
        List<NaverNewsResponse.ArticleItem> firstPage = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            firstPage.add(validItem(i));
        }
        expectPage(1, 101, firstPage);
        server.expect(requestTo(pageUrl(101))).andRespond(withServerError());

        // when
        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> adapter.search("AI"));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        assertThat(thrown).hasCauseInstanceOf(RestClientException.class);
        server.verify();
    }

    private NaverNewsResponse.ArticleItem validItem(int id) {
        return new NaverNewsResponse.ArticleItem(
                "기사 " + id, "https://example.com/" + id, null, "요약", DATE);
    }

    private void expectPage(int start, int total, List<NaverNewsResponse.ArticleItem> items)
            throws Exception {
        NaverNewsResponse response = new NaverNewsResponse(DATE, total, start, items.size(), items);
        server.expect(requestTo(pageUrl(start)))
                .andRespond(
                        withSuccess(
                                new ObjectMapper().writeValueAsString(response),
                                MediaType.APPLICATION_JSON));
    }

    private String pageUrl(int start) {
        return "https://example.com/news?query=AI&display=100&start="
                + start
                + "&sort=date&format=json";
    }
}
