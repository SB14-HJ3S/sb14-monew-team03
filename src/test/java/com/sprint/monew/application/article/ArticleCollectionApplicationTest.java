package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import com.sprint.monew.application.article.required.NaverNewsPort;
import com.sprint.monew.application.article.required.RssNewsPort;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.application.interest.provided.dto.InterestKeywordResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleCollectionApplicationTest {
    @Mock private ArticleService articles;
    @Mock private ArticleInterestService links;
    @Mock private InterestService interestService;
    @Mock private RssNewsPort rss;
    @Mock private NaverNewsPort naver;
    @Mock private ArticleEventPublisherPort eventPublisher;
    @InjectMocks private ArticleCollectionApplication application;

    @Test
    @DisplayName("관심사별 전체 키워드를 반환하고 같은 키워드의 관심사도 구분한다.")
    void findValidInterestKeywords() {
        Interest first = Interest.create("기술", List.of("AI", "Java"));
        Interest second = Interest.create("경제", List.of("AI"));
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(first, second));
        var result = application.findValidInterestKeywords();
        assertThat(result)
                .extracting(InterestKeywordResult::interest)
                .containsExactly(first, second);
        assertThat(result)
                .extracting(InterestKeywordResult::keywords)
                .containsExactly(List.of("AI", "Java"), List.of("AI"));
    }

    @Test
    @DisplayName("반환된 키워드 목록은 조회 시점의 값을 유지하고 외부에서 수정할 수 없다.")
    void givenInterestKeywordsChange_thenKeepCollectedTargets() {
        Interest interest = Interest.create("기술", List.of("AI", "Java"));
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(interest));

        var target = application.findValidInterestKeywords().get(0);
        interest.updateKeywords(List.of("Spring"));

        assertThat(target.keywords()).containsExactly("AI", "Java");
        assertThatThrownBy(() -> target.keywords().add("Python"))
                .isInstanceOf(UnsupportedOperationException.class);
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
    @DisplayName("저장 건수는 신규 기사만 세고 알림 건수는 새 연결만 세며 삭제 기사는 제외한다.")
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
        when(links.createIfAbsent(saved, interest)).thenReturn(true);
        when(links.createIfAbsent(existingEntity, interest)).thenReturn(true);

        assertThat(application.saveArticles(interest, List.of(fresh, existing, deleted)))
                .isEqualTo(1);

        verify(articles).save(any());
        verify(links).createIfAbsent(saved, interest);
        verify(links).createIfAbsent(existingEntity, interest);
        verifyNoMoreInteractions(links);
        verify(eventPublisher).publish(new ArticlesSavedEvent(interest.getId(), "기술", 2));
    }

    @Test
    @DisplayName("같은 URL이 두 관심사에 매칭되면 각 관심사에 새 연결 1건을 알린다.")
    void givenSameArticleForTwoInterests_thenNotifyBothInterests() {
        Interest first = Interest.create("기술", List.of("AI"));
        Interest second = Interest.create("경제", List.of("AI"));
        var item = collected("shared", "AI", "");
        Article saved = item.toArticle();
        when(articles.findBySourceUrl("shared")).thenReturn(Optional.empty(), Optional.of(saved));
        when(articles.save(any())).thenReturn(saved);
        when(links.createIfAbsent(saved, first)).thenReturn(true);
        when(links.createIfAbsent(saved, second)).thenReturn(true);

        assertThat(application.saveArticles(first, List.of(item))).isEqualTo(1);
        assertThat(application.saveArticles(second, List.of(item))).isZero();

        verify(articles).save(any());
        verify(eventPublisher).publish(new ArticlesSavedEvent(first.getId(), "기술", 1));
        verify(eventPublisher).publish(new ArticlesSavedEvent(second.getId(), "경제", 1));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    @DisplayName("한 관심사에 같은 기사가 중복되면 새 연결만 한 번 센다.")
    void givenDuplicateArticle_thenCountOnlyNewLink() {
        Interest interest = Interest.create("기술", List.of("AI"));
        var item = collected("shared", "AI", "");
        Article existing = item.toArticle();
        when(articles.findBySourceUrl("shared")).thenReturn(Optional.of(existing));
        when(links.createIfAbsent(existing, interest)).thenReturn(true, false);

        assertThat(application.saveArticles(interest, List.of(item, item))).isZero();

        verify(eventPublisher).publish(new ArticlesSavedEvent(interest.getId(), "기술", 1));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    @DisplayName("이미 연결된 기사만 재수집하면 알림을 다시 발행하지 않는다.")
    void givenExistingLink_thenDoNotNotifyAgain() {
        Interest interest = Interest.create("기술", List.of("AI"));
        var item = collected("existing", "AI", "");
        Article existing = item.toArticle();
        when(articles.findBySourceUrl("existing")).thenReturn(Optional.of(existing));

        assertThat(application.saveArticles(interest, List.of(item))).isZero();

        verify(links).createIfAbsent(existing, interest);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenEmptyArticles_thenReturnZero() {
        assertThat(application.saveArticles(Interest.create("기술", List.of("AI")), List.of()))
                .isZero();
        verifyNoInteractions(articles, links, eventPublisher);
    }

    @Test
    @DisplayName("관심사 연결 실패를 숨기지 않고 호출자에게 전달한다.")
    void givenLinkFailure_thenPropagate() {
        Interest interest = Interest.create("기술", List.of("AI"));
        var item = collected("url", "AI", "");
        Article saved = item.toArticle();
        var failure = new IllegalStateException("link failed");
        when(articles.save(any())).thenReturn(saved);
        when(links.createIfAbsent(saved, interest)).thenThrow(failure);
        assertThatThrownBy(() -> application.saveArticles(interest, List.of(item)))
                .isSameAs(failure);
        verifyNoInteractions(eventPublisher);
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
