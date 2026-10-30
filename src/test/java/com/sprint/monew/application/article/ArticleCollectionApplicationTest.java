package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleCollectionApplicationTest {
    @Mock private ArticleService articles;
    @Mock private ArticleInterestService links;
    @Mock private InterestService interestService;
    @InjectMocks private ArticleCollectionApplication application;

    @Test
    @DisplayName("신규 기사만 저장하고 삭제 기사는 건너뛰며 신규 기사 수로 이벤트를 발행한다.")
    void givenNewExistingAndDeletedArticles_whenSaveArticlesAndLink_thenSaveOnlyNewArticles() {
        // given
        Interest interest = interest();
        Article created = mock(Article.class);
        Article existing = mock(Article.class);
        Article deleted = mock(Article.class);
        when(deleted.isDeleted()).thenReturn(true);
        when(articles.findBySourceUrl("new")).thenReturn(Optional.empty());
        when(articles.findBySourceUrl("existing")).thenReturn(Optional.of(existing));
        when(articles.findBySourceUrl("deleted")).thenReturn(Optional.of(deleted));
        when(articles.createCollectedArticle(any())).thenReturn(created);

        // when
        application.saveArticlesAndLink(
                List.of(collected("new"), collected("existing"), collected("deleted")), interest);

        // then
        verify(articles).createCollectedArticle(collected("new"));
        verify(links).getOrCreateArticleInterest(created, interest);
        verify(links).getOrCreateArticleInterest(existing, interest);
        verify(links, never()).getOrCreateArticleInterest(deleted, interest);
        verify(articles).publishArticlesSaved(interest.getId(), interest.getName(), 1);
    }

    @Test
    @DisplayName("수집 결과가 없거나 기존 기사뿐이면 알림 이벤트를 발행하지 않는다.")
    void givenEmptyOrExistingArticles_whenSaveArticlesAndLink_thenDoNotPublishEvent() {
        // given
        Interest interest = interest();
        Article existing = mock(Article.class);
        when(articles.findBySourceUrl("existing")).thenReturn(Optional.of(existing));

        // when
        application.saveArticlesAndLink(List.of(), interest);
        application.saveArticlesAndLink(List.of(collected("existing")), interest);

        // then
        verify(links).getOrCreateArticleInterest(existing, interest);
        verify(articles, never()).createCollectedArticle(any());
        verify(articles, never()).publishArticlesSaved(any(), any(), anyInt());
    }

    @Test
    @DisplayName("관심사 연결에 실패하면 성공 이벤트 없이 예외를 전달한다.")
    void givenLinkFailure_whenSaveArticlesAndLink_thenPropagateWithoutPublishing() {
        // given
        Interest interest = interest();
        Article article = mock(Article.class);
        when(articles.findBySourceUrl("new")).thenReturn(Optional.empty());
        when(articles.createCollectedArticle(any())).thenReturn(article);
        when(links.getOrCreateArticleInterest(article, interest))
                .thenThrow(new IllegalStateException("link failed"));

        // when
        Throwable thrown =
                catchThrowable(
                        () -> application.saveArticlesAndLink(List.of(collected("new")), interest));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("link failed");
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
