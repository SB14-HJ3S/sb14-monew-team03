package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.*;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleCollectionApplicationTest {
    @Mock private ArticleService articles;
    @Mock private ArticleInterestService links;
    @Mock private InterestService interestService;
    @Mock private RssNewsPort rss;
    @Mock private NaverNewsPort naver;
    @InjectMocks private ArticleCollectionApplication application;

    @Test
    @DisplayName("조회된 관심사를 키워드별로 펼치고 같은 키워드의 관심사도 구분한다.")
    void findValidInterestKeywords() {
        Interest first = Interest.create("기술", List.of("AI", "Java"));
        Interest second = Interest.create("경제", List.of("AI"));
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(first, second));
        var result = application.findValidInterestKeywords();
        assertThat(result).hasSize(3);
        assertThat(result)
                .filteredOn(item -> item.interest() == first)
                .extracting(item -> item.keyword())
                .containsExactlyInAnyOrder("AI", "Java");
        assertThat(result)
                .filteredOn(item -> item.interest() == second)
                .extracting(item -> item.keyword())
                .containsExactly("AI");
    }

    @Test
    void givenNoInterests_thenReturnEmptyTargets() {
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of());
        assertThat(application.findValidInterestKeywords()).isEmpty();
    }

    @Test
    @DisplayName("네이버는 전달한 키워드만 검색하고 동일 URL의 첫 기사를 유지한다.")
    void collectNaverArticles() {
        var first = collected("same", "첫 기사", "");
        var other = collected("other", "다른 기사", "");
        when(naver.search("AI")).thenReturn(List.of(first, collected("same", "중복", ""), other));
        assertThat(application.collectNaverArticles("AI")).containsExactly(first, other);
        verify(naver).search("AI");
        verifyNoInteractions(rss, articles, links);
    }

    @ParameterizedTest
    @EnumSource(
            value = ArticleSourceType.class,
            names = {"HANKYUNG", "CHOSUN", "YEONHAP"})
    @DisplayName("요청한 RSS 출처만 수집하고 URL 중복을 제거한다.")
    void collectRssArticles(ArticleSourceType source) {
        var first = collected("same", "첫 기사", "");
        when(rss.collect(source)).thenReturn(List.of(first, collected("same", "중복", "")));
        assertThat(application.collectRssArticles(source)).containsExactly(first);
        verify(rss).collect(source);
        verifyNoMoreInteractions(rss);
        verifyNoInteractions(naver, articles, links);
    }

    @Test
    @DisplayName("수집 실패는 Batch가 처리할 수 있도록 그대로 전달한다.")
    void givenCollectionFailure_thenPropagate() {
        var failure = new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        when(naver.search("AI")).thenThrow(failure);
        when(rss.collect(ArticleSourceType.YEONHAP)).thenThrow(failure);
        assertThatThrownBy(() -> application.collectNaverArticles("AI")).isSameAs(failure);
        assertThatThrownBy(() -> application.collectRssArticles(ArticleSourceType.YEONHAP))
                .isSameAs(failure);
    }

    @ParameterizedTest
    @CsvSource({"ai 뉴스,날씨,true", "경제,새로운 Ai 기술,true", "경제,날씨,false"})
    @DisplayName("제목 또는 요약에 관심사 키워드가 포함되는지 대소문자를 무시하고 확인한다.")
    void matchesInterest(String title, String summary, boolean expected) {
        Interest interest = Interest.create("기술", List.of(" Java ", " AI "));
        assertThat(application.matchesInterest(collected("url", title, summary), interest))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("신규 기사만 건수를 세고 기존 기사는 연결하며 삭제 기사는 제외한다.")
    void saveArticles() {
        Interest interest = Interest.create("기술", List.of("AI"));
        var fresh = collected("new", "AI", "");
        var existing = collected("existing", "AI", "");
        var deleted = collected("deleted", "AI", "");
        Article saved = fresh.toArticle();
        Article existingEntity = existing.toArticle();
        Article deletedEntity = deleted.toArticle();
        deletedEntity.softDelete();
        when(articles.findBySourceUrl("new")).thenReturn(Optional.empty());
        when(articles.findBySourceUrl("existing")).thenReturn(Optional.of(existingEntity));
        when(articles.findBySourceUrl("deleted")).thenReturn(Optional.of(deletedEntity));
        when(articles.save(any())).thenReturn(saved);
        assertThat(application.saveArticles(interest, List.of(fresh, existing, deleted)))
                .isEqualTo(1);
        verify(articles).save(any());
        verify(links).getOrCreateArticleInterest(saved, interest);
        verify(links).getOrCreateArticleInterest(existingEntity, interest);
        verifyNoMoreInteractions(links);
    }

    @Test
    void givenEmptyArticles_thenReturnZero() {
        assertThat(application.saveArticles(Interest.create("기술", List.of("AI")), List.of()))
                .isZero();
        verifyNoInteractions(articles, links);
    }

    @Test
    @DisplayName("관심사 연결 실패를 숨기지 않고 호출자에게 전달한다.")
    void givenLinkFailure_thenPropagate() {
        Interest interest = Interest.create("기술", List.of("AI"));
        var item = collected("url", "AI", "");
        Article saved = item.toArticle();
        var failure = new IllegalStateException("link failed");
        when(articles.save(any())).thenReturn(saved);
        when(links.getOrCreateArticleInterest(saved, interest)).thenThrow(failure);
        assertThatThrownBy(() -> application.saveArticles(interest, List.of(item)))
                .isSameAs(failure);
    }

    private CollectedArticle collected(String url, String title, String summary) {
        return new CollectedArticle(
                ArticleSourceType.HANKYUNG,
                url,
                title,
                LocalDateTime.of(2026, 10, 30, 12, 0),
                summary);
    }
}
