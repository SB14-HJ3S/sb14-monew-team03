package com.sprint.monew.adapter.out.article.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @BeforeEach
    void setUp() {
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
}
