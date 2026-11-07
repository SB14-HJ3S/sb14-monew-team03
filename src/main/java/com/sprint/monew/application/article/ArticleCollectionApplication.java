package com.sprint.monew.application.article;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.article.provided.service.ArticleInterestService;
import com.sprint.monew.application.article.provided.service.ArticleService;
import com.sprint.monew.application.article.provided.usecase.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.usecase.dto.CollectedArticle;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import com.sprint.monew.application.article.required.NaverNewsPort;
import com.sprint.monew.application.article.required.RssNewsPort;
import com.sprint.monew.application.interest.provided.service.InterestService;
import com.sprint.monew.application.interest.provided.usecase.dto.InterestKeywordResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.type.ArticleSourceType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
    private final ArticleEventPublisherPort eventPublisher;

    // Reader - 구독자가 1명 이상이고, 키워드를 1개 이상 가지고 있는 키워드 조회
    @Override
    @Transactional(readOnly = true)
    public List<InterestKeywordResult> findValidInterestKeywords() {
        return interestService.findSubscribedWithKeywords().stream()
                .map(InterestKeywordResult::from)
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
        // articles에 같은 url 있는지 확인
        List<CollectedArticle> uniqueArticles = removeDuplicateArticles(articles);
        if (uniqueArticles.isEmpty()) {
            return 0;
        }

        List<String> urls = uniqueArticles.stream().map(CollectedArticle::sourceUrl).toList();

        // 이번 기사들의 url로 기존에 저장되어 있는 기사들 한번에 조회
        Map<String, Article> existingUrl = new HashMap<>();
        for (Article article : articleService.findAllBySourceUrls(urls)) {
            existingUrl.put(article.getSourceUrl(), article);
        }

        // 기존 기사 중 이미 현재 관심사와 연결되어있던 기사 id조회
        List<UUID> existingArticleInterests =
                existingUrl.values().stream()
                        .filter(article -> !article.isDeleted())
                        .map(Article::getId)
                        .toList();

        Set<UUID> linkedArticleIds =
                existingArticleInterests.isEmpty()
                        ? Set.of()
                        : articleInterestService.findLinkedArticleIds(
                                interest.getId(), existingArticleInterests);

        int newArticleCount = 0;
        List<Article> articlesToLink = new ArrayList<>();

        for (CollectedArticle collectedArticle : uniqueArticles) {
            Article existing = existingUrl.get(collectedArticle.sourceUrl());
            if (Objects.isNull(existing)) {
                Article saved = articleService.save(collectedArticle.toArticle());
                newArticleCount++;
                articlesToLink.add(saved);
                continue;
            }

            if (existing.isDeleted()) {
                continue;
            }

            if (!linkedArticleIds.contains(existing.getId())) {
                articlesToLink.add(existing);
            }
        }

        articleInterestService.createLinks(interest, articlesToLink);
        int newLinkedArticleCount = articlesToLink.size();

        if (newLinkedArticleCount > 0) {
            eventPublisher.publish(
                    new ArticlesSavedEvent(
                            interest.getId(), interest.getName(), newLinkedArticleCount));
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
}
