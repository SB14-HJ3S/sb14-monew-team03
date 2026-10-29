package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleCollectionApplicationTest {
    private final ArticleService articles = mock(ArticleService.class);
    private final ArticleInterestService links = mock(ArticleInterestService.class);
    private final ArticleCollectionApplication application =
            new ArticleCollectionApplication(mock(InterestService.class), articles, links);

    @Test
    void countsOnlyNewDatabaseArticlesAndSkipsDeletedArticles() {
        Interest interest = interest();
        Article created = mock(Article.class);
        Article existing = mock(Article.class);
        Article deleted = mock(Article.class);
        when(deleted.isDeleted()).thenReturn(true);
        when(articles.findBySourceUrl("new")).thenReturn(Optional.empty());
        when(articles.findBySourceUrl("existing")).thenReturn(Optional.of(existing));
        when(articles.findBySourceUrl("deleted")).thenReturn(Optional.of(deleted));
        when(articles.createCollectedArticle(any())).thenReturn(created);

        application.saveArticlesAndLink(
                List.of(collected("new"), collected("existing"), collected("deleted")), interest);

        verify(articles).createCollectedArticle(collected("new"));
        verify(links).getOrCreateArticleInterest(created, interest);
        verify(links).getOrCreateArticleInterest(existing, interest);
        verify(links, never()).getOrCreateArticleInterest(deleted, interest);
        verify(articles).publishArticlesSaved(interest.getId(), interest.getName(), 1);
    }

    @Test
    void emptyOrExistingOnlyCollectionDoesNotPublish() {
        Interest interest = interest();
        Article existing = mock(Article.class);
        when(articles.findBySourceUrl("existing")).thenReturn(Optional.of(existing));

        application.saveArticlesAndLink(List.of(), interest);
        application.saveArticlesAndLink(List.of(collected("existing")), interest);

        verify(links).getOrCreateArticleInterest(existing, interest);
        verify(articles, never()).createCollectedArticle(any());
        verify(articles, never()).publishArticlesSaved(any(), any(), anyInt());
    }

    @Test
    void linkFailurePropagatesWithoutPublishingSuccessEvent() {
        Interest interest = interest();
        Article article = mock(Article.class);
        when(articles.findBySourceUrl("new")).thenReturn(Optional.empty());
        when(articles.createCollectedArticle(any())).thenReturn(article);
        when(links.getOrCreateArticleInterest(article, interest))
                .thenThrow(new IllegalStateException("link failed"));

        assertThatThrownBy(
                        () -> application.saveArticlesAndLink(List.of(collected("new")), interest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("link failed");
        verify(articles, never()).publishArticlesSaved(any(), any(), anyInt());
    }

    private Interest interest() {
        Interest interest = Interest.create("커피", List.of("커피"));
        ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());
        return interest;
    }

    private CollectedArticle collected(String url) {
        return new CollectedArticle(
                ArticleSourceType.NAVER, url, "기사", LocalDateTime.of(2026, 10, 29, 12, 0), "요약");
    }
}
