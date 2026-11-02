package com.sprint.monew.adapter.in.batch.article.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.interest.provided.dto.InterestKeywordResult;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ArticleCollectProcessorMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ArticleCollectionUseCase useCase = mock(ArticleCollectionUseCase.class);
    private final ArticleCollectProcessor processor =
            new ArticleCollectProcessor(registry, useCase);

    @AfterEach
    void closeRegistry() {
        registry.close();
    }

    @Test
    @DisplayName("RSS는 캐시 적재 시에만, 네이버는 키워드 호출마다 성공을 기록하며 빈 응답도 성공이다")
    void countSuccessfulFetchesWithoutCountingCacheHits() {
        Interest first = Interest.create("기술", List.of("AI", "Java"));
        Interest second = Interest.create("경제", List.of("AI"));
        var article = article();
        when(useCase.collectRssArticles(ArticleSourceType.HANKYUNG)).thenReturn(List.of(article));
        when(useCase.matchesInterest(any(), any())).thenReturn(true);

        assertThat(processor.process(InterestKeywordResult.from(first)).articles())
                .containsExactly(article);
        assertThat(processor.process(InterestKeywordResult.from(second)).articles())
                .containsExactly(article);

        for (ArticleSourceType source : ArticleSourceType.values()) {
            assertThat(count(ArticleCollectionMetrics.FETCH_SUCCESS, source))
                    .isEqualTo(source == ArticleSourceType.NAVER ? 3 : 1);
            assertThat(count(ArticleCollectionMetrics.FETCH_FAILED, source)).isZero();
            if (source != ArticleSourceType.NAVER) {
                verify(useCase).collectRssArticles(source);
            }
        }
        verify(useCase, times(2)).collectNaverArticles("AI");
        verify(useCase).collectNaverArticles("Java");
    }

    @ParameterizedTest
    @EnumSource(ArticleSourceType.class)
    @DisplayName("수집이 실패한 출처는 실패만 기록하고 성공으로 세지 않는다")
    void doNotCountFailedFetchAsSuccess(ArticleSourceType source) {
        var failure = new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        if (source == ArticleSourceType.NAVER) {
            when(useCase.collectNaverArticles("AI")).thenThrow(failure);
        } else {
            when(useCase.collectRssArticles(source)).thenThrow(failure);
        }

        processor.process(InterestKeywordResult.from(Interest.create("기술", List.of("AI"))));

        assertThat(count(ArticleCollectionMetrics.FETCH_SUCCESS, source)).isZero();
        assertThat(count(ArticleCollectionMetrics.FETCH_FAILED, source)).isEqualTo(1);
    }

    @Test
    @DisplayName("수집 이후 관심사 매칭이 실패해도 이미 성공한 수집은 성공으로 기록한다")
    void retainFetchSuccessWhenMatchingFails() {
        var failure = new MonewException(CommonErrorType.INVALID_REQUEST);
        when(useCase.collectRssArticles(ArticleSourceType.HANKYUNG)).thenReturn(List.of(article()));
        when(useCase.matchesInterest(any(), any())).thenThrow(failure);

        assertThatThrownBy(
                        () ->
                                processor.process(
                                        InterestKeywordResult.from(
                                                Interest.create("기술", List.of("AI")))))
                .isSameAs(failure);

        assertThat(count(ArticleCollectionMetrics.FETCH_SUCCESS, ArticleSourceType.HANKYUNG))
                .isEqualTo(1);
        assertThat(count(ArticleCollectionMetrics.FETCH_FAILED, ArticleSourceType.HANKYUNG))
                .isZero();
    }

    private double count(String name, ArticleSourceType source) {
        var counter = registry.find(name).tag("source", source.name()).counter();
        return counter == null ? 0 : counter.count();
    }

    private CollectedArticle article() {
        return new CollectedArticle(
                ArticleSourceType.HANKYUNG,
                "https://example.com/news",
                "AI 뉴스",
                LocalDateTime.of(2026, 11, 3, 12, 0),
                "AI 뉴스 요약");
    }
}
