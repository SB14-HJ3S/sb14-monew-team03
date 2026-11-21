package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.adapter.in.batch.article.collection.dto.InterestCollectedArticles;
import com.sprint.monew.application.article.provided.usecase.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.usecase.dto.CollectedArticle;
import com.sprint.monew.application.interest.provided.usecase.dto.InterestKeywordResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ArticleCollectProcessor
        implements ItemProcessor<InterestKeywordResult, InterestCollectedArticles> {

    private static final List<ArticleSourceType> RSS_SOURCES =
            List.of(
                    ArticleSourceType.HANKYUNG,
                    ArticleSourceType.CHOSUN,
                    ArticleSourceType.YEONHAP);

    private final MeterRegistry meterRegistry;
    private final ArticleCollectionUseCase useCase;
    private final ArticleCollectionTiming timing;
    private final Map<ArticleSourceType, List<CollectedArticle>> rssFetched = new HashMap<>();

    @Override
    public InterestCollectedArticles process(InterestKeywordResult interestKeyword) {
        Map<String, CollectedArticle> articlesByUrl = new LinkedHashMap<>();

        // RSS를 최초 수집 후 chunk마다 재사용
        // 키워드 매칭되는 기사만 수집
        for (ArticleSourceType source : RSS_SOURCES) {
            try {
                List<CollectedArticle> rssArticles = fetchRssIfAbsent(source);
                for (CollectedArticle article : rssArticles) {
                    if (useCase.matchesInterest(article, interestKeyword.interest())) {
                        articlesByUrl.putIfAbsent(article.sourceUrl(), article);
                    }
                }
            } catch (MonewException e) {
                if (!e.getErrorType().equals(ExternalErrorType.EXTERNAL_API_CALL_FAILED)) {
                    throw e;
                }
                increaseFetchFailCount(source);
                log.warn("RSS {} 수집 실패.", source, e);
            }
        }

        // 네이버 기사는 키워드로 바로 쿼리해오므로 바로 삽입
        for (String keyword : interestKeyword.keywords()) {
            try {
                List<CollectedArticle> fetched =
                        timing.measureFetch(
                                ArticleSourceType.NAVER,
                                () -> useCase.collectNaverArticles(keyword));
                increaseFetchSuccessCount(ArticleSourceType.NAVER);
                increaseFetchedArticleCount(ArticleSourceType.NAVER, fetched.size());
                for (CollectedArticle article : fetched) {
                    articlesByUrl.putIfAbsent(article.sourceUrl(), article);
                }
            } catch (MonewException e) {
                if (!e.getErrorType().equals(ExternalErrorType.EXTERNAL_API_CALL_FAILED)) {
                    throw e;
                }
                increaseFetchFailCount(ArticleSourceType.NAVER);
                log.warn("네이버 수집 실패. keyword={}", keyword, e);
            }
        }

        return InterestCollectedArticles.of(
                interestKeyword.interest(), List.copyOf(articlesByUrl.values()));
    }

    private void increaseFetchSuccessCount(ArticleSourceType sourceType) {
        meterRegistry
                .counter(ArticleCollectionMetrics.FETCH_SUCCESS, "source", sourceType.name())
                .increment();
    }

    private void increaseFetchFailCount(ArticleSourceType key) {
        meterRegistry
                .counter(ArticleCollectionMetrics.FETCH_FAILED, "source", key.name())
                .increment();
    }

    private void increaseFetchedArticleCount(ArticleSourceType sourceType, int fetchedCount) {
        meterRegistry
                .counter(ArticleCollectionMetrics.FETCHED_ARTICLES, "source", sourceType.name())
                .increment(fetchedCount);
    }

    private List<CollectedArticle> fetchRssIfAbsent(ArticleSourceType sourceType) {
        return rssFetched.computeIfAbsent(
                sourceType,
                key -> {
                    List<CollectedArticle> fetched =
                            timing.measureFetch(key, () -> useCase.collectRssArticles(key));
                    increaseFetchedArticleCount(sourceType, fetched.size());
                    increaseFetchSuccessCount(sourceType);
                    return fetched;
                });
    }
}
