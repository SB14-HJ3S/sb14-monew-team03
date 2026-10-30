package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import com.sprint.monew.adapter.out.article.ArticleJpaRepository;
import com.sprint.monew.adapter.out.interest.InterestJpaRepository;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.*;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@IntegrationTest
class ArticleCollectionIntegrationTest {
    @Autowired private ArticleCollectionUseCase collectionUseCase;
    @Autowired private ArticleJpaRepository articles;
    @Autowired private InterestJpaRepository interests;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private RssNewsPort rss;
    @MockitoBean private NaverNewsPort naver;
    @MockitoBean private InterestService interestService;
    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> interestIds = new ArrayList<>();
    private final String url = "https://example.com/collection/" + UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        jdbc.update(
                "delete from article_interests where article_id in "
                        + "(select id from articles where source_url like ?)",
                url + "%");
        jdbc.update("delete from articles where source_url like ?", url + "%");
        for (UUID interestId : interestIds) {
            jdbc.update("delete from notifications where resource_id = ?", interestId);
            jdbc.update("delete from article_interests where interest_id = ?", interestId);
            jdbc.update("delete from subscriptions where interest_id = ?", interestId);
            interests.deleteById(interestId);
        }
        for (UUID userId : userIds) {
            jdbc.update("delete from users where id = ?", userId);
        }
    }

    @Test
    @DisplayName("외부 수집은 트랜잭션 없이 실행하고 반복 수집해도 기사와 연결이 중복되지 않는다.")
    void givenRepeatedCollection_whenCollectArticles_thenPersistOneArticleAndLink() {
        // given
        Interest interest = saveInterest(List.of("AI"));
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(interest));
        when(rss.collect(ArticleSourceType.HANKYUNG))
                .thenAnswer(
                        invocation -> {
                            assertThat(
                                            TransactionSynchronizationManager
                                                    .isActualTransactionActive())
                                    .isFalse();
                            return List.of(collected(), collected());
                        });
        when(naver.search("AI"))
                .thenAnswer(
                        invocation -> {
                            assertThat(
                                            TransactionSynchronizationManager
                                                    .isActualTransactionActive())
                                    .isFalse();
                            return List.of(collected());
                        });
        // when
        collectionUseCase.collectArticles();
        collectionUseCase.collectArticles();
        // then
        assertThat(articles.findBySourceUrl(url)).isPresent();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from articles where source_url = ?",
                                Long.class,
                                url))
                .isEqualTo(1L);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from article_interests where interest_id = ?",
                                Long.class,
                                interest.getId()))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("RSS 저장이 롤백되어도 네이버와 다음 관심사의 저장을 계속한다.")
    void givenInvalidRssArticle_whenCollectArticles_thenRollbackRssAndContinue() {
        // given
        Interest first = saveInterest(List.of("AI"));
        Interest second = saveInterest(List.of("AI"));
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(first, second));
        CollectedArticle invalid =
                new CollectedArticle(
                        ArticleSourceType.HANKYUNG,
                        url + "/invalid",
                        null,
                        collected().publishedAt(),
                        "AI");
        when(rss.collect(ArticleSourceType.HANKYUNG)).thenReturn(List.of(collected(), invalid));
        when(naver.search("AI"))
                .thenReturn(
                        List.of(
                                new CollectedArticle(
                                        ArticleSourceType.NAVER,
                                        url + "/naver",
                                        "AI",
                                        collected().publishedAt(),
                                        "")));
        // when
        collectionUseCase.collectArticles();
        // then
        assertThat(articles.findBySourceUrl(url)).isEmpty();
        assertThat(articles.findBySourceUrl(url + "/invalid")).isEmpty();
        assertThat(articles.findBySourceUrl(url + "/naver")).isPresent();
        for (Interest interest : List.of(first, second)) {
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from article_interests where interest_id = ?",
                                    Long.class,
                                    interest.getId()))
                    .isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("관심사에 신규 기사가 저장되면 구독자에게 알림을 생성한다.")
    void givenNewArticle_whenCollectArticles_thenCreateSubscriberNotification() {
        // given
        Interest interest = saveInterest(List.of("AI"));
        when(interestService.findSubscribedWithKeywords()).thenReturn(List.of(interest));
        when(rss.collect(ArticleSourceType.HANKYUNG)).thenReturn(List.of(collected()));
        // when
        collectionUseCase.collectArticles();
        // then
        assertThat(articles.findBySourceUrl(url)).isPresent();
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(notificationCount()).isEqualTo(1L));
    }

    private long notificationCount() {
        return userIds.stream()
                .mapToLong(
                        id ->
                                jdbc.queryForObject(
                                        "select count(*) from notifications where user_id = ?",
                                        Long.class,
                                        id))
                .sum();
    }

    private Interest saveInterest(List<String> keywords) {
        Interest interest =
                interests.saveAndFlush(Interest.create("수집-" + UUID.randomUUID(), keywords));
        interestIds.add(interest.getId());
        UUID userId = UUID.randomUUID();
        userIds.add(userId);
        jdbc.update(
                "insert into users (id, created_at, email, nickname, password) values (?, now(), ?, ?, ?)",
                userId,
                userId + "@example.com",
                "독자",
                "test-password");
        jdbc.update(
                "insert into subscriptions (id, created_at, user_id, interest_id) values (?, now(), ?, ?)",
                UUID.randomUUID(),
                userId,
                interest.getId());
        jdbc.update("update interests set subscriber_count = 1 where id = ?", interest.getId());
        return interest;
    }

    private CollectedArticle collected() {
        return new CollectedArticle(
                ArticleSourceType.NAVER,
                url,
                "AI 기사",
                LocalDateTime.of(2026, 10, 27, 12, 0),
                "인공지능 소식");
    }
}
