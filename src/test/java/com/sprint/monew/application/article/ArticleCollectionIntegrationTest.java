package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.adapter.out.repository.article.ArticleJpaRepository;
import com.sprint.monew.adapter.out.repository.interest.InterestJpaRepository;
import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.article.provided.usecase.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.usecase.dto.CollectedArticle;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@RecordApplicationEvents
class ArticleCollectionIntegrationTest {
    @Autowired private ArticleCollectionUseCase collectionUseCase;
    @Autowired private ArticleJpaRepository articles;
    @Autowired private InterestJpaRepository interests;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEvents events;
    private final List<UUID> interestIds = new ArrayList<>();
    private final String url = "https://example.com/collection/" + UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        jdbc.update(
                "delete from article_interests where article_id in (select id from articles where source_url like ?)",
                url + "%");
        jdbc.update("delete from articles where source_url like ?", url + "%");
        interestIds.forEach(interests::deleteById);
    }

    @Test
    @DisplayName("반복 저장은 기사를 재사용하고 관심사 연결도 중복되지 않는다.")
    void givenRepeatedSave_thenPersistOneArticleAndLink() {
        Interest first = saveInterest();
        Interest second = saveInterest();
        assertThat(collectionUseCase.saveArticles(first, List.of(collected()))).isEqualTo(1);
        assertThat(collectionUseCase.saveArticles(first, List.of(collected()))).isZero();
        assertThat(collectionUseCase.saveArticles(second, List.of(collected()))).isZero();
        assertThat(articles.findBySourceUrl(url)).isPresent();
        assertThat(linkCount(first)).isEqualTo(1);
        assertThat(linkCount(second)).isEqualTo(1);
        assertThat(events.stream(ArticlesSavedEvent.class))
                .containsExactly(
                        new ArticlesSavedEvent(first.getId(), first.getName(), 1),
                        new ArticlesSavedEvent(second.getId(), second.getName(), 1));
    }

    @Test
    @DisplayName("목록의 기사 저장이 실패하면 앞서 저장한 기사와 연결도 함께 롤백한다.")
    void givenInvalidArticle_thenRollbackWholeSave() {
        Interest interest = saveInterest();
        var invalid =
                new CollectedArticle(
                        ArticleSourceType.HANKYUNG,
                        url + "/invalid",
                        null,
                        collected().publishedAt(),
                        "AI");
        assertThatThrownBy(
                        () ->
                                collectionUseCase.saveArticles(
                                        interest, List.of(collected(), invalid)))
                .isInstanceOf(RuntimeException.class);
        assertThat(articles.findBySourceUrl(url)).isEmpty();
        assertThat(articles.findBySourceUrl(url + "/invalid")).isEmpty();
        assertThat(linkCount(interest)).isZero();
    }

    @Test
    @DisplayName("저장은 바깥 chunk 트랜잭션에 참여하여 바깥 롤백 시 기사와 연결도 취소된다.")
    void givenOuterRollback_thenRollbackArticlesAndLinks() {
        Interest interest = saveInterest();
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            assertThat(
                                            collectionUseCase.saveArticles(
                                                    interest, List.of(collected())))
                                    .isEqualTo(1);
                            status.setRollbackOnly();
                        });
        assertThat(articles.findBySourceUrl(url)).isEmpty();
        assertThat(linkCount(interest)).isZero();
    }

    @Test
    @DisplayName("바깥 chunk가 커밋되면 기사와 관심사 연결이 유지된다.")
    void givenOuterCommit_thenPersistArticlesAndLinks() {
        Interest interest = saveInterest();
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                assertThat(
                                                collectionUseCase.saveArticles(
                                                        interest, List.of(collected())))
                                        .isEqualTo(1));
        assertThat(articles.findBySourceUrl(url)).isPresent();
        assertThat(linkCount(interest)).isEqualTo(1);
    }

    private long linkCount(Interest interest) {
        return jdbc.queryForObject(
                "select count(*) from article_interests where interest_id = ?",
                Long.class,
                interest.getId());
    }

    private Interest saveInterest() {
        Interest interest =
                interests.saveAndFlush(Interest.create("수집-" + UUID.randomUUID(), List.of("AI")));
        interestIds.add(interest.getId());
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
