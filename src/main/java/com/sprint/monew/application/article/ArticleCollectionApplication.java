package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.NaverNewsPort;
import com.sprint.monew.application.article.required.RssNewsPort;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.application.interest.provided.dto.InterestKeywordResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.type.ArticleSourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class ArticleCollectionApplication implements ArticleCollectionUseCase {

    private final InterestService interestService;
    private final ArticleService articleService;
    private final ArticleInterestService articleInterestService;
    private final NaverNewsPort naverNewsPort;
    private final RssNewsPort rssNewsPort;

    // Reader - 구독자가 1명 이상이고, 키워드를 1개 이상 가지고 있는 키워드 조회
    @Override
    @Transactional(readOnly = true)
    public List<InterestKeywordResult> findValidInterestKeywords() {
        return interestService.findSubscribedWithKeywords().stream()
                .flatMap(
                        interest ->
                                interest.getKeywordNames().stream()
                                        .map(
                                                keyword ->
                                                        new InterestKeywordResult(
                                                                interest, keyword)))
                .toList();
    }

    // Processor 1 - 키워드 하나에 대한 모든 네이버 기사 수집
    @Override
    public List<CollectedArticle> collectNaverArticles(String keyword) {
        return removeDuplicateArticles(naverNewsPort.search(keyword));
    }

    // Processor 2 - 출처 하나에 대해 RSS 기사 수집
    @Override
    public List<CollectedArticle> collectRssArticles(ArticleSourceType source) {
        return removeDuplicateArticles(rssNewsPort.collect(source));
    }

    // Writer - Transaction안에. 기사 저장, 관심사와연결
    // 저장된 기사 수 반환
    @Override
    @Transactional
    public int saveArticles(Interest interest, List<CollectedArticle> articles) {

        int newArticleCount = 0;

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
                newArticleCount++;
            }

            articleInterestService.getOrCreateArticleInterest(article, interest);
        }

        return newArticleCount;
    }

    // 기사 제목이나 요약에 관심사 키워드 중 하나라도 있으면 return true, else false
    @Override
    public boolean matchesInterest(CollectedArticle article, Interest interest) {
        String text = (article.title() + " " + article.summary()).toLowerCase();
        return interest.getKeywordNames().stream()
                .anyMatch(keyword -> text.contains(keyword.trim().toLowerCase()));
    }

    // 한번의 수집 안에 같은 URL로 수집한 기사 있는지 체크
    private List<CollectedArticle> removeDuplicateArticles(List<CollectedArticle> articles) {

        Map<String, CollectedArticle> byUrl = new LinkedHashMap<>();

        for (CollectedArticle article : articles) {
            byUrl.putIfAbsent(article.sourceUrl(), article);
        }

        return new ArrayList<>(byUrl.values());
    }

    // 참고용: 나중에 batch에서 알림 발행하기
    //    private void publishArticlesSaved(Interest interest, int articleCount) {
    //        // 트랜잭션으로 감싸지 않으면 publishArticlesSaved가 트랜잭션 밖에서 실행되고,
    //        //      AFTER_COMMIT 리스너는 이벤트를 무시
    //        transactionTemplate.execute(
    //            status -> {
    // 알림 호출 여기서!!
    //                eventPublisherPort.publish(
    //                    new ArticlesSavedEvent(
    //                        interest.getId(), interest.getName(), articleCount));
    //                return null;
    //            });
    //    }
}
