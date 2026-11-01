package com.sprint.monew.application.article;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleCollectionReport;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.provided.dto.SourceResult;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import com.sprint.monew.application.article.required.NaverNewsPort;
import com.sprint.monew.application.article.required.RssNewsPort;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.type.ArticleSourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@ApplicationService
public class ArticleCollectionApplication implements ArticleCollectionUseCase {

    private static final List<ArticleSourceType> RSS_SOURCES =
            List.of(
                    ArticleSourceType.HANKYUNG,
                    ArticleSourceType.CHOSUN,
                    ArticleSourceType.YEONHAP);

    private final InterestService interestService;
    private final ArticleService articleService;
    private final ArticleInterestService articleInterestService;

    private final NaverNewsPort naverNewsPort;
    private final RssNewsPort rssNewsPort;
    private final ArticleEventPublisherPort eventPublisherPort;

    private final TransactionTemplate transactionTemplate;

    public ArticleCollectionApplication(
            InterestService interestService,
            ArticleService articleService,
            ArticleInterestService articleInterestService,
            NaverNewsPort naverNewsPort,
            RssNewsPort rssNewsPort,
            ArticleEventPublisherPort eventPublisherPort,
            @Qualifier("articleSaveTransactionTemplate") TransactionTemplate transactionTemplate) {
        this.interestService = interestService;
        this.articleService = articleService;
        this.articleInterestService = articleInterestService;
        this.naverNewsPort = naverNewsPort;
        this.rssNewsPort = rssNewsPort;
        this.eventPublisherPort = eventPublisherPort;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public ArticleCollectionReport collectArticles() {
        List<Interest> validInterests = interestService.findSubscribedWithKeywords();

        CollectionOutcome rssResult = collectRssArticles();
        List<CollectedArticle> rssArticles = rssResult.articles();

        int naverCollectedCount = 0;
        int naverFailureCount = 0;
        int totalNewArticleCount = 0;
        int saveFailureCount = 0;

        for (Interest interest : validInterests) {
            // RSS - 관심사 키워드 매칭된 기사만 연결
            List<CollectedArticle> matchedRssArticles =
                    rssArticles.stream()
                            .filter(article -> matchesInterest(article, interest))
                            .toList();

            log.info(
                    "RSS 매칭: interest={}, keywords={}, matched={}/{}",
                    interest.getName(),
                    interest.getKeywordNames(),
                    matchedRssArticles.size(),
                    rssArticles.size());

            SaveOutcome rssSave = saveArticleInterestInTransaction(matchedRssArticles, interest);

            // Naver - 관심사 키워드로 검색해서 연결
            CollectionOutcome naverResult = collectNaverArticles(interest);
            List<CollectedArticle> naverArticles = naverResult.articles();
            SaveOutcome naverSave = saveArticleInterestInTransaction(naverArticles, interest);

            // 관심사에 대한 기사 수집 작업에 대한 데이터 집계
            int newCount = rssSave.newArticleCount() + naverSave.newArticleCount();
            naverCollectedCount += naverResult.articles().size();
            naverFailureCount += naverResult.failureCount();
            totalNewArticleCount += newCount;
            saveFailureCount += rssSave.failureCount() + naverSave.failureCount();

            // 1 관심사 1 알림 생성 (1 사용자에 대해)
            if (newCount > 0) {
                publishArticlesSaved(interest, newCount);
            }
        }
        return new ArticleCollectionReport(
                new SourceResult(naverCollectedCount, naverFailureCount),
                new SourceResult(rssResult.articles().size(), rssResult.failureCount()),
                totalNewArticleCount,
                saveFailureCount);
    }

    // ---------- 수집 ----------
    private record CollectionOutcome(List<CollectedArticle> articles, int failureCount) {}

    // ArticleService로 뺄 수 있지만 CollectedArticle이 /provided의 dto여서 일단 여기로 뺌
    private CollectionOutcome collectRssArticles() {
        Map<String, CollectedArticle> byUrl = new LinkedHashMap<>(); // url 중복 제거
        int failureCount = 0;
        for (ArticleSourceType source : RSS_SOURCES) {
            try {
                for (CollectedArticle article : rssNewsPort.collect(source)) {
                    byUrl.putIfAbsent(article.sourceUrl(), article);
                }
            } catch (MonewException e) {
                failureCount++;
                log.error("RSS 수집 실패: source={}", source, e);
            }
        }
        return new CollectionOutcome(new ArrayList<>(byUrl.values()), failureCount);
    }

    private CollectionOutcome collectNaverArticles(Interest interest) {
        Map<String, CollectedArticle> byUrl = new LinkedHashMap<>(); // url 중복 제거
        int failureCount = 0;

        for (String keyword : interest.getKeywordNames()) {
            try {
                for (CollectedArticle article : naverNewsPort.search(keyword)) {
                    byUrl.putIfAbsent(article.sourceUrl(), article);
                }
            } catch (MonewException e) {
                failureCount++;
                log.error("NAVER 수집 실패: interest={}, keyword={}", interest.getName(), keyword, e);
            }
        }
        return new CollectionOutcome(new ArrayList<>(byUrl.values()), failureCount);
    }

    private boolean matchesInterest(CollectedArticle article, Interest interest) {
        String text = (article.title() + " " + article.summary()).toLowerCase();
        return interest.getKeywordNames().stream()
                .anyMatch(keyword -> text.contains(keyword.trim().toLowerCase()));
    }

    // ---------- 저장 ----------
    private record SaveOutcome(int newArticleCount, int failureCount) {}

    // 저장한 기사 수 + 저장 실패한 기사 수 (=SaveOutcome) 반환
    private SaveOutcome saveArticleInterestInTransaction(
            List<CollectedArticle> articles, Interest interest) {

        int newArticleCount = 0;
        int failureCount = 0;

        for (CollectedArticle collected : articles) {
            try {
                Integer count =
                        transactionTemplate.execute(
                                status -> saveOneArticleInterest(collected, interest));

                newArticleCount += count != null ? count : 0;
            } catch (RuntimeException e) {
                failureCount++;
                log.error(
                        "기사 저장 실패: interest={}, sourceUrl={}",
                        interest.getName(),
                        collected.sourceUrl(),
                        e);
            }
        }

        return new SaveOutcome(newArticleCount, failureCount);
    }

    // 저장한 기사 수 반환
    private int saveOneArticleInterest(CollectedArticle collected, Interest interest) {

        Optional<Article> existing = articleService.findBySourceUrl(collected.sourceUrl());

        Article article;
        int newCount = 0;

        if (existing.isPresent()) {
            article = existing.get();

            if (article.isDeleted()) {
                return 0;
            }
        } else {
            article = articleService.save(collected.toArticle());
            newCount = 1;
        }

        articleInterestService.getOrCreateArticleInterest(article, interest);

        return newCount;
    }

    // ---------- 이벤트 ----------

    private void publishArticlesSaved(Interest interest, int articleCount) {
        // 트랜잭션으로 감싸지 않으면 publishArticlesSaved가 트랜잭션 밖에서 실행되고,
        //      AFTER_COMMIT 리스너는 이벤트를 무시
        transactionTemplate.execute(
                status -> {
                    eventPublisherPort.publish(
                            new ArticlesSavedEvent(
                                    interest.getId(), interest.getName(), articleCount));
                    return null;
                });
    }
}
