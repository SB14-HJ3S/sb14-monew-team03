package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.adapter.in.batch.ArticleCollectionRunner;
import com.sprint.monew.adapter.out.article.ArticleJpaRepository;
import com.sprint.monew.adapter.out.interest.InterestJpaRepository;
import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import com.sprint.monew.application.article.required.NaverNewsPort;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ArticleCollectionIntegrationTest {

    @Autowired private ArticleCollectionUseCase collectionUseCase;
    @Autowired private ArticleCollectionRunner runner;
    @Autowired private InterestJpaRepository interests;
    @Autowired private ArticleJpaRepository articles;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private NaverNewsPort naver;
    @Autowired private TransactionTemplate transactionTemplate;
    @MockitoSpyBean private ArticleEventPublisherPort eventPublisher;

    @BeforeEach
    void verifyEventIsPublishedInsideTransaction() {
        doAnswer(
                        invocation -> {
                            assertThat(
                                            TransactionSynchronizationManager
                                                    .isActualTransactionActive())
                                    .isTrue();
                            return invocation.callRealMethod();
                        })
                .when(eventPublisher)
                .publish(any(ArticlesSavedEvent.class));
    }

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
    void collectionDoesNotWriteAndRunnerPersistsDistinctLinksWithoutHttpTransaction() {
        saveInterest(List.of("AI", "인공지능"));
        saveInterest(List.of("AI"));
        CollectedArticle article = collected();

        for (String keyword : List.of("AI", "인공지능")) {
            when(naver.search(keyword))
                    .thenAnswer(
                            invocation -> {
                                assertThat(
                                                TransactionSynchronizationManager
                                                        .isActualTransactionActive())
                                        .isFalse();
                                return List.of(article, article);
                            });
        }

        List<Interest> targets = collectionUseCase.findCollectionInterests();
        assertThat(targets).hasSize(2);
        for (Interest target : targets) {
            assertThat(collectionUseCase.collectNaverArticles(target)).containsExactly(article);
        }
        assertThat(articles.findBySourceUrl(url)).isEmpty();

        runner.runCollection();
        runner.runCollection();

        UUID articleId = articles.findBySourceUrl(url).orElseThrow().getId();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from article_interests where article_id = ?",
                                Long.class,
                                articleId))
                .isEqualTo(2L);
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(notificationCount()).isEqualTo(1L));
        verify(naver, times(6)).search("AI");
        verify(naver, times(3)).search("인공지능");
    }

    @Test
    void linkFailureRollsBackNewArticle() {
        Interest missing = Interest.create("없는 관심사", List.of("AI"));
        ReflectionTestUtils.setField(missing, "id", UUID.randomUUID());

        assertThatThrownBy(
                        () -> collectionUseCase.saveArticlesAndLink(List.of(collected()), missing))
                .isInstanceOf(RuntimeException.class);

        assertThat(articles.findBySourceUrl(url)).isEmpty();
    }

    @Test
    void collectionTargetsRequirePositiveSubscriberCount() {
        Interest unsubscribed =
                interests.saveAndFlush(Interest.create("미구독-" + UUID.randomUUID(), List.of("경제")));
        interestIds.add(unsubscribed.getId());
        assertThat(collectionUseCase.findCollectionInterests()).isEmpty();
        saveInterest(List.of("AI"));
        assertThat(collectionUseCase.findCollectionInterests()).hasSize(1);
        jdbc.update("update interests set subscriber_count = 0 where id = ?", interestIds.get(1));
        assertThat(collectionUseCase.findCollectionInterests()).isEmpty();
    }

    @Test
    void publishesOneEventForNewArticlesAndNoneOnRepeat() {
        Interest interest = saveInterest(List.of("AI"));
        CollectedArticle first = collected();
        CollectedArticle second =
                new CollectedArticle(
                        first.source(),
                        url + "/second",
                        "두 번째 기사",
                        first.publishedAt(),
                        first.summary());

        collectionUseCase.saveArticlesAndLink(List.of(first, first, second), interest);
        collectionUseCase.saveArticlesAndLink(List.of(first, second), interest);

        verify(eventPublisher, times(1))
                .publish(new ArticlesSavedEvent(interest.getId(), interest.getName(), 2));
        verify(eventPublisher, times(1)).publish(any(ArticlesSavedEvent.class));
        assertThat(articles.findBySourceUrl(url)).isPresent();
        assertThat(articles.findBySourceUrl(url + "/second")).isPresent();
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            assertThat(notificationCount()).isEqualTo(1L);
                            assertThat(
                                            jdbc.queryForObject(
                                                    "select content from notifications where resource_id = ?",
                                                    String.class,
                                                    interest.getId()))
                                    .isEqualTo(interest.getName() + "와 관련된 기사가 2건 등록되었습니다.");
                            assertThat(
                                            jdbc.queryForObject(
                                                    "select user_id from notifications where resource_id = ?",
                                                    UUID.class,
                                                    interest.getId()))
                                    .isEqualTo(userIds.get(0));
                        });
    }

    @Test
    void laterInvalidArticleRollsBackEarlierArticleAndLink() {
        Interest interest = saveInterest(List.of("AI"));
        CollectedArticle first = collected();
        CollectedArticle invalid =
                new CollectedArticle(
                        first.source(),
                        url + "/invalid",
                        null,
                        first.publishedAt(),
                        first.summary());

        assertThatThrownBy(
                        () ->
                                collectionUseCase.saveArticlesAndLink(
                                        List.of(first, invalid), interest))
                .isInstanceOf(RuntimeException.class);

        assertThat(articles.findBySourceUrl(url)).isEmpty();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from article_interests where interest_id = ?",
                                Long.class,
                                interest.getId()))
                .isZero();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void publishedEventDoesNotCreateNotificationWhenTransactionRollsBack() {
        Interest interest = saveInterest(List.of("AI"));
        transactionTemplate.executeWithoutResult(
                status -> {
                    collectionUseCase.saveArticlesAndLink(List.of(collected()), interest);
                    verify(eventPublisher)
                            .publish(
                                    new ArticlesSavedEvent(
                                            interest.getId(), interest.getName(), 1));
                    assertThat(notificationCount()).isZero();
                    status.setRollbackOnly();
                });

        assertThat(articles.findBySourceUrl(url)).isEmpty();
        await().during(Duration.ofMillis(500))
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(notificationCount()).isZero());
    }

    @Test
    void notificationIsCreatedOnlyAfterOuterTransactionCommits() {
        Interest interest = saveInterest(List.of("AI"));
        transactionTemplate.executeWithoutResult(
                status -> {
                    collectionUseCase.saveArticlesAndLink(List.of(collected()), interest);
                    await().during(Duration.ofMillis(300))
                            .atMost(Duration.ofSeconds(2))
                            .untilAsserted(() -> assertThat(notificationCount()).isZero());
                });
        await().atMost(Duration.ofSeconds(5))
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
