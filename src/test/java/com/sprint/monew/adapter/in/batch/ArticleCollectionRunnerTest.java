package com.sprint.monew.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionSystemException;

@ExtendWith(MockitoExtension.class)
class ArticleCollectionRunnerTest {
    @Mock private ArticleCollectionUseCase useCase;
    @InjectMocks private ArticleCollectionRunner runner;
    private final Logger logger = (Logger) LoggerFactory.getLogger(ArticleCollectionRunner.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void detachLogs() {
        logger.detachAppender(logs);
        logs.stop();
    }

    static Stream<Arguments> failures() {
        return Stream.of(
                Arguments.of(
                        new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED), "기사 수집 실패"),
                Arguments.of(new DataIntegrityViolationException("value too long"), "기사 저장 실패"),
                Arguments.of(new TransactionSystemException("commit failed"), "기사 저장 실패"),
                Arguments.of(
                        new IllegalStateException("unexpected failure"), "기사 수집 중 예상하지 못한 오류"));
    }

    @ParameterizedTest
    @MethodSource("failures")
    @DisplayName("관심사 수집에 실패하면 저장을 건너뛰고 로그를 남긴 뒤 다음 관심사를 처리한다.")
    void givenCollectionFailure_whenRunCollection_thenLogAndContinue(
            RuntimeException failure, String message) {
        // given
        Interest first = interest("첫 관심사");
        Interest second = interest("다음 관심사");
        List<CollectedArticle> articles = List.of(article());
        when(useCase.findCollectionInterests()).thenReturn(List.of(first, second));
        when(useCase.collectNaverArticles(first)).thenThrow(failure);
        when(useCase.collectNaverArticles(second)).thenReturn(articles);

        // when
        runner.runCollection();

        // then

        verify(useCase, never()).saveArticlesAndLink(anyList(), eq(first));
        InOrder order = inOrder(useCase);
        order.verify(useCase).collectNaverArticles(first);
        order.verify(useCase).collectNaverArticles(second);
        order.verify(useCase).saveArticlesAndLink(articles, second);
        assertFailureLog(first, failure, message);
    }

    @ParameterizedTest
    @MethodSource("failures")
    @DisplayName("관심사 기사 저장에 실패하면 로그를 남긴 뒤 다음 관심사를 처리한다.")
    void givenSaveFailure_whenRunCollection_thenLogAndContinue(
            RuntimeException failure, String message) {
        // given
        Interest first = interest("첫 관심사");
        Interest second = interest("다음 관심사");
        List<CollectedArticle> articles = List.of(article());
        when(useCase.findCollectionInterests()).thenReturn(List.of(first, second));
        when(useCase.collectNaverArticles(any())).thenReturn(articles);
        doThrow(failure).when(useCase).saveArticlesAndLink(articles, first);

        // when
        runner.runCollection();

        // then

        InOrder order = inOrder(useCase);
        order.verify(useCase).saveArticlesAndLink(articles, first);
        order.verify(useCase).collectNaverArticles(second);
        order.verify(useCase).saveArticlesAndLink(articles, second);
        assertFailureLog(first, failure, message);
    }

    @Test
    @DisplayName("수집 대상 관심사 조회에 실패하면 예외를 전달하고 수집을 시작하지 않는다.")
    void givenTargetLookupFailure_whenRunCollection_thenPropagateException() {
        // given
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("lookup failed");
        when(useCase.findCollectionInterests()).thenThrow(failure);

        // when
        Throwable thrown = catchThrowable(runner::runCollection);

        // then
        assertThat(thrown).isSameAs(failure);

        verify(useCase).findCollectionInterests();
        verifyNoMoreInteractions(useCase);
    }

    private void assertFailureLog(Interest interest, RuntimeException failure, String message) {
        assertThat(logs.list).hasSize(1);
        ILoggingEvent event = logs.list.get(0);
        assertThat(event.getFormattedMessage()).contains(message, interest.getId().toString());
        assertThat(((ThrowableProxy) event.getThrowableProxy()).getThrowable()).isSameAs(failure);
        if (failure instanceof MonewException monew) {
            assertThat(event.getFormattedMessage()).contains(monew.getErrorType().toString());
        }
    }

    private Interest interest(String name) {
        Interest interest = Interest.create(name, List.of(name));
        ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());
        return interest;
    }

    private CollectedArticle article() {
        return new CollectedArticle(
                ArticleSourceType.NAVER,
                "https://example.com/news",
                "기사",
                LocalDateTime.of(2026, 10, 29, 12, 0),
                "요약");
    }
}
