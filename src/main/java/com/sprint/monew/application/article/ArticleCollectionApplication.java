package com.sprint.monew.application.article;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@ApplicationService
@RequiredArgsConstructor
public class ArticleCollectionApplication implements ArticleCollectionUseCase {

    private static final List<ArticleSourceType> RSS_SOURCES =
            List.of(ArticleSourceType.HANKYUNG, ArticleSourceType.CHOSUN, ArticleSourceType.YONHAP);

    private final InterestService interestService;
    private final ArticleService articleService;
    private final ArticleInterestService articleInterestService;

    private final NaverNewsPort naverNewsPort;
    private final RssNewsPort rssNewsPort;
    private final ArticleEventPublisherPort eventPublisherPort;

    private final TransactionTemplate transactionTemplate;

    @Override
    public void collectArticles() {
        List<Interest> validInterests = interestService.findSubscribedWithKeywords();
        List<CollectedArticle> rssArticles = collectRssArticles();

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

            int newCount = saveArticleInterestInTransaction(matchedRssArticles, interest);

            // Naver - 관심사 키워드로 검색해서 연결
            List<CollectedArticle> naverArticles = collectNaverArticles(interest);
            newCount += saveArticleInterestInTransaction(naverArticles, interest);

            // 1 관심사 1 알림 생성 (1 사용자에 대해)
            if (newCount > 0) {
                publishArticlesSaved(interest, newCount);
            }
        }
    }

    // ---------- 수집 ----------
    // ArticleService로 뺄 수 있지만 CollectedArticle이 /provided의 dto여서 일단 여기로 뺌
    private List<CollectedArticle> collectRssArticles() {
        Map<String, CollectedArticle> byUrl = new LinkedHashMap<>(); // url 중복 제거
        for (ArticleSourceType source : RSS_SOURCES) {
            try {
                for (CollectedArticle article : rssNewsPort.collect(source)) {
                    byUrl.putIfAbsent(article.sourceUrl(), article);
                }
            } catch (MonewException e) {
                log.error("RSS 수집 실패: source={}", source, e);
            }
        }
        return new ArrayList<>(byUrl.values());
    }

    private List<CollectedArticle> collectNaverArticles(Interest interest) {
        Map<String, CollectedArticle> byUrl = new LinkedHashMap<>(); // url 중복 제거
        for (String keyword : interest.getKeywordNames()) {
            try {
                for (CollectedArticle article : naverNewsPort.search(keyword)) {
                    byUrl.putIfAbsent(article.sourceUrl(), article);
                }
            } catch (MonewException e) {
                log.error("NAVER 수집 실패: interest={}, keyword={}", interest.getName(), keyword, e);
            }
        }
        return new ArrayList<>(byUrl.values());
    }

    private boolean matchesInterest(CollectedArticle article, Interest interest) {
        String text = (article.title() + " " + article.summary()).toLowerCase();
        return interest.getKeywordNames().stream()
                .anyMatch(keyword -> text.contains(keyword.trim().toLowerCase()));
    }

    // ---------- 저장 ----------
    // 저장할때만 트랜잭션으로 감싼다
    private int saveArticleInterestInTransaction(
            List<CollectedArticle> articles, Interest interest) {
        try {
            Integer count =
                    transactionTemplate.execute(status -> saveArticleInterest(articles, interest));
            return count != null ? count : 0;
        } catch (RuntimeException e) {
            log.error("기사 저장 실패: interest={}", interest.getName(), e);
            return 0;
        }
    }

    private int saveArticleInterest(List<CollectedArticle> articles, Interest interest) {
        int newCount = 0;
        for (CollectedArticle collected : articles) {
            Optional<Article> existing = articleService.findBySourceUrl(collected.sourceUrl());
            Article article;
            if (existing.isPresent()) {
                article = existing.get();
                if (article.isDeleted()) {
                    continue;
                }
            } else {
                article = articleService.save(collected.toArticle());
                newCount++;
            }
            articleInterestService.getOrCreateArticleInterest(article, interest);
        }
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
