package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.interest.entity.InterestKeyword;
import com.sprint.monew.global.annotation.ApplicationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ApplicationService
@RequiredArgsConstructor
public class ArticleCollectionApplication implements ArticleCollectionUseCase {

    private final InterestService interestService;
    private final ArticleService articleService;
    private final ArticleInterestService articleInterestService;

    @Override
    @Transactional
    public void saveArticlesAndLink(List<CollectedArticle> articles, Interest interest) {
        int newArticleCount = 0;

        for (CollectedArticle collectedArticle : articles) {
            Optional<Article> existing =
                    articleService.findBySourceUrl(collectedArticle.sourceUrl());
            Article article;
            if (existing.isPresent()) {
                article = existing.get();
                if (article.isDeleted()) {
                    continue;
                }
            } else {
                article = articleService.createCollectedArticle(collectedArticle);
                newArticleCount++;
            }
            articleInterestService.getOrCreateArticleInterest(article, interest);
        }

        if (newArticleCount > 0) {
            // 관심사 기사 추가 알림 이벤트 발행
            articleService.publishArticlesSaved(
                    interest.getId(), interest.getName(), newArticleCount);
        }
    }

    @Override
    public List<Interest> findCollectionInterests() {
        return interestService.findSubscribedWithKeywords();
    }

    @Override
    public List<CollectedArticle> collectNaverArticles(Interest interest) {
        List<CollectedArticle> articles = new ArrayList<>();
        Set<String> collectedUrls = new HashSet<>(); // url 중복 수집 방지

        for (InterestKeyword keyword : interest.getKeywords()) {
            List<CollectedArticle> searched =
                    articleService.collectNaverArticles(keyword.getKeyword().trim());

            for (CollectedArticle article : searched) {
                if (!collectedUrls.contains(article.sourceUrl())) {
                    articles.add(article);
                    collectedUrls.add(article.sourceUrl());
                }
            }
        }
        return articles;
    }
}
