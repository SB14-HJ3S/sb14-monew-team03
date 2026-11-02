package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.adapter.in.batch.article.collection.dto.InterestCollectedArticles;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.interest.provided.dto.InterestKeywordResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
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

    private final ArticleCollectionUseCase useCase;
    private final Map<ArticleSourceType, List<CollectedArticle>> rssFetched = new HashMap<>();

    @Override
    public InterestCollectedArticles process(InterestKeywordResult interestKeyword) {
        Map<String, CollectedArticle> articlesByUrl = new LinkedHashMap<>();

        // RSS를 최초 수집 후 chunk마다 재사용
        // 키워드 매칭되는 기사만 수집
        for (ArticleSourceType source : RSS_SOURCES) {
            try {
                List<CollectedArticle> rssArticles =
                        rssFetched.computeIfAbsent(source, useCase::collectRssArticles);

                for (CollectedArticle article : rssArticles) {
                    if (useCase.matchesInterest(article, interestKeyword.interest())) {
                        articlesByUrl.putIfAbsent(article.sourceUrl(), article);
                    }
                }
            } catch (MonewException e) {
                if (e.getErrorType().equals(ExternalErrorType.EXTERNAL_API_CALL_FAILED)) {
                    throw e;
                }

                log.warn("RSS {} 수집 실패.", source, e);
            }
        }

        // 네이버 기사는 키워드로 바로 쿼리해오므로 바로 삽입
        for (String keyword : interestKeyword.keywords()) {
            try {
                for (CollectedArticle article : useCase.collectNaverArticles(keyword)) {
                    articlesByUrl.putIfAbsent(article.sourceUrl(), article);
                }
            } catch (MonewException e) {
                if (e.getErrorType().equals(ExternalErrorType.EXTERNAL_API_CALL_FAILED)) {
                    throw e;
                }

                log.warn("네이버 수집 실패. keyword={}", keyword);
            }
        }

        return InterestCollectedArticles.of(
                interestKeyword.interest(), List.copyOf(articlesByUrl.values()));
    }
}
