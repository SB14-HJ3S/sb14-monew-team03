package com.sprint.monew.adapter.out.article.rss;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class RssNewsAdapterTest {
    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.Builder builder;

    private MockRestServiceServer server;
    private RssNewsAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder http = RestClient.builder();
        server = MockRestServiceServer.bindTo(http).build();
        when(builder.clone()).thenReturn(builder);
        when(builder.build()).thenReturn(http.build());
        adapter =
                new RssNewsAdapter(
                        builder,
                        new RssNewsMapper(),
                        "https://example.com/hankyung",
                        "https://example.com/chosun",
                        "https://example.com/yonhap");
    }

    @ParameterizedTest
    @CsvSource({
        "HANKYUNG,hankyung,Fri 30 Oct 2026 12:00:00 +0900",
        "CHOSUN,chosun,Fri 30 Oct 2026 03:00:00 GMT",
        "YONHAP,yonhap,Fri 30 Oct 2026 12:00:00 +0900"
    })
    @DisplayName("언론사별 설정 URL로 요청하고 RSS 날짜·HTML 요약을 변환한다.")
    void givenSourceFeed_whenCollect_thenParseArticles(
            ArticleSourceType source, String path, String date) {
        // given
        server.expect(requestTo("https://example.com/" + path))
                .andRespond(
                        withSuccess(
                                feed(
                                        item(
                                                "제목",
                                                "https://example.com/news",
                                                date.replace("Fri ", "Fri, "))),
                                MediaType.APPLICATION_XML));
        // when
        var result = adapter.collect(source);
        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).source()).isEqualTo(source);
        assertThat(result.get(0).publishedAt()).isEqualTo(LocalDateTime.of(2026, 10, 30, 12, 0));
        assertThat(result.get(0).summary()).isEqualTo("AI & 뉴스");
        server.verify();
    }

    @Test
    @DisplayName("기사 한 건의 변환이 실패해도 앞뒤 정상 기사를 유지한다.")
    void givenInvalidItem_whenCollect_thenSkipOnlyInvalidArticle() {
        // given
        String date = "Fri, 30 Oct 2026 12:00:00 +0900";
        server.expect(requestTo("https://example.com/hankyung"))
                .andRespond(
                        withSuccess(
                                feed(
                                        item("첫 기사", "https://example.com/1", date)
                                                + item("잘못된 기사", "", date)
                                                + item("다음 기사", "https://example.com/2", date)),
                                MediaType.APPLICATION_XML));
        // when
        var result = adapter.collect(ArticleSourceType.HANKYUNG);
        // then
        assertThat(result)
                .extracting(a -> a.sourceUrl())
                .containsExactly("https://example.com/1", "https://example.com/2");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not xml", "<rss>"})
    @DisplayName("빈 응답 또는 잘못된 XML은 외부 호출 오류로 전달한다.")
    void givenInvalidBody_whenCollect_thenThrowMonewException(String body) {
        // given
        server.expect(requestTo("https://example.com/hankyung"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_XML));
        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class, () -> adapter.collect(ArticleSourceType.HANKYUNG));
        // then
        assertThat(thrown.getErrorType()).isEqualTo(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        server.verify();
    }

    private String feed(String items) {
        return "<?xml version='1.0' encoding='UTF-8'?><rss version='2.0'><channel><title>뉴스</title><link>https://example.com</link><description>뉴스 피드</description>"
                + items
                + "</channel></rss>";
    }

    private String item(String title, String link, String date) {
        return "<item><title>"
                + title
                + "</title><link>"
                + link
                + "</link><pubDate>"
                + date
                + "</pubDate><description><![CDATA[<p>AI &amp; 뉴스</p>]]></description></item>";
    }
}
