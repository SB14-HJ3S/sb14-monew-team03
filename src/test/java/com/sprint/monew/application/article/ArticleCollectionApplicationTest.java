package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;
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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ArticleCollectionApplicationTest {
    @Mock private ArticleService articles;
    @Mock private ArticleInterestService links;
    @Mock private InterestService interestService;
    @Mock private RssNewsPort rss;
    @Mock private NaverNewsPort naver;
    @Mock private ArticleEventPublisherPort events;
    @Mock private TransactionTemplate transactions;
    @InjectMocks private ArticleCollectionApplication application;

    @BeforeEach
    void executeTransactionCallback() {
        lenient()
                .when(transactions.execute(any()))
                .thenAnswer(
                        invocation -> {
                            TransactionCallback<?> callback = invocation.getArgument(0);
                            return callback.doInTransaction(mock(TransactionStatus.class));
                        });
    }

    @Test
    @DisplayName("RSS 제목·요약을 대소문자 구분 없이 매칭하고 중복 URL은 한 번 저장한다.")
    void givenMatchingAndDuplicateRss_whenCollectArticles_thenSaveDistinctMatches() {
        // given
        Interest interest = interest("AI", "AI");
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(interest));
        CollectedArticle title = collected("title", "ai 뉴스", "");
        CollectedArticle summary = collected("summary", "다른 제목", "AI 소식");
        when(rss.collect(ArticleSourceType.HANKYUNG))
                .thenReturn(List.of(title, summary, collected("other", "날씨", "맑음")));
        when(rss.collect(ArticleSourceType.CHOSUN)).thenReturn(List.of(title));
        when(articles.save(any())).thenAnswer(i -> i.getArgument(0));
        // when
        application.collectArticles();
        // then
        verify(articles, times(2)).save(any());
        verify(articles).findBySourceUrl("title");
        verify(articles).findBySourceUrl("summary");
        verify(articles, never()).findBySourceUrl("other");
        verify(links, times(2)).getOrCreateArticleInterest(any(), eq(interest));
        verify(events).publish(new ArticlesSavedEvent(interest.getId(), interest.getName(), 2));
        for (ArticleSourceType source :
                List.of(
                        ArticleSourceType.HANKYUNG,
                        ArticleSourceType.CHOSUN,
                        ArticleSourceType.YONHAP)) {
            verify(rss).collect(source);
        }
    }

    @Test
    @DisplayName("RSS와 네이버에 같은 URL이 있으면 기존 기사를 재사용하고 신규 건수만 합산한다.")
    void givenOverlappingSources_whenCollectArticles_thenPublishCombinedNewCountOnce() {
        // given
        Interest interest = interest("AI", "AI", "인공지능");
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(interest));
        CollectedArticle shared = collected("shared", "AI", "");
        Article saved = shared.toArticle();
        when(rss.collect(ArticleSourceType.HANKYUNG)).thenReturn(List.of(shared));
        when(naver.search("AI")).thenReturn(List.of(shared, collected("naver", "기사", "")));
        when(naver.search("인공지능")).thenReturn(List.of(shared));
        when(articles.findBySourceUrl("shared")).thenReturn(Optional.empty(), Optional.of(saved));
        when(articles.save(any())).thenAnswer(i -> i.getArgument(0));
        // when
        application.collectArticles();
        // then
        verify(articles, times(2)).save(any());
        verify(events).publish(new ArticlesSavedEvent(interest.getId(), interest.getName(), 2));
        verifyNoMoreInteractions(events);
    }

    @Test
    @DisplayName("기존 기사는 연결만 하고 삭제 기사는 건너뛰며 신규 이벤트를 발행하지 않는다.")
    void givenExistingAndDeletedArticles_whenCollectArticles_thenLinkExistingOnly() {
        // given
        Interest interest = interest("AI", "AI");
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(interest));
        CollectedArticle existing = collected("existing", "AI", "");
        CollectedArticle deleted = collected("deleted", "AI", "");
        Article existingEntity = existing.toArticle();
        Article deletedEntity = deleted.toArticle();
        deletedEntity.softDelete();
        when(naver.search("AI")).thenReturn(List.of(existing, deleted));
        when(articles.findBySourceUrl("existing")).thenReturn(Optional.of(existingEntity));
        when(articles.findBySourceUrl("deleted")).thenReturn(Optional.of(deletedEntity));
        // when
        application.collectArticles();
        // then
        verify(links).getOrCreateArticleInterest(existingEntity, interest);
        verifyNoMoreInteractions(links);
        verify(articles, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    @DisplayName("언론사와 키워드 하나가 실패해도 나머지 출처의 기사를 저장한다.")
    void givenSourceFailures_whenCollectArticles_thenContinueOtherSources() {
        // given
        Interest interest = interest("AI", "실패", "AI");
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(interest));
        when(rss.collect(ArticleSourceType.HANKYUNG))
                .thenThrow(new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED));
        when(rss.collect(ArticleSourceType.CHOSUN)).thenReturn(List.of(collected("rss", "AI", "")));
        when(naver.search("실패"))
                .thenThrow(new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED));
        when(naver.search("AI")).thenReturn(List.of(collected("naver", "AI", "")));
        when(articles.save(any())).thenAnswer(i -> i.getArgument(0));
        // when
        application.collectArticles();
        // then
        verify(articles, times(2)).save(any());
        verify(rss).collect(ArticleSourceType.YONHAP);
        verify(events).publish(new ArticlesSavedEvent(interest.getId(), interest.getName(), 2));
    }

    private Interest interest(String name, String... keywords) {
        Interest interest = Interest.create(name, List.of(keywords));
        ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());
        return interest;
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
