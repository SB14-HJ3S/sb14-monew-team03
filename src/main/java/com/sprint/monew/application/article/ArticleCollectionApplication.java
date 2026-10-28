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
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class ArticleCollectionApplication implements ArticleCollectionUseCase {

    private final InterestService interestService;
    private final IArticleService articleService;
    private final IArticleInterestService articleInterestService;

    @Override
    @Transactional
    public void saveAndLink(CollectedArticle collectedArticle, Interest interest) {
        // CollectedArticle과 Interest를 ArticleInterest로 저장
        Optional<Article> saved = articleService.saveCollectedArticle(collectedArticle);
        if (saved.isPresent()) {
            articleInterestService.getOrCreateArticleInterest(saved.get(), interest);
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
