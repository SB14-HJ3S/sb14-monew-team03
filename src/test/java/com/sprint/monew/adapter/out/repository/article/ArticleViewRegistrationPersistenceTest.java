package com.sprint.monew.adapter.out.repository.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.application.article.ArticleApplication;
import com.sprint.monew.application.article.provided.dto.ArticleViewStatusResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("기사 조회 기록 등록 통합 테스트")
class ArticleViewRegistrationPersistenceTest {

    private static final String USER_HEADER = "Monew-Request-User-ID";

    @Autowired private ArticleViewStatusRepositoryAdapter viewRepository;
    @Autowired private ArticleApplication articleApplication;
    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;
    private UUID articleId;
    private final List<UUID> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(
                status -> {
                    Article article =
                            Article.create(
                                    ArticleSourceType.NAVER,
                                    "https://example.com/" + UUID.randomUUID(),
                                    "기사",
                                    LocalDateTime.now(),
                                    "요약");
                    entityManager.persist(article);
                    articleId = article.getId();
                    for (int i = 0; i < 4; i++) {
                        User user =
                                User.create(
                                        UUID.randomUUID() + "@example.com", "사용자", "password1!");
                        entityManager.persist(user);
                        userIds.add(user.getId());
                    }
                });
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM article_view_statuses WHERE article_id = ?", articleId);
        jdbcTemplate.update("DELETE FROM articles WHERE id = ?", articleId);
        userIds.forEach(id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id));
    }

    private ArticleViewStatusResult register(UUID userId) {
        RequestIdContext.set(userId);
        try {
            return articleApplication.register(articleId);
        } finally {
            RequestIdContext.clear();
        }
    }

    private List<ArticleViewStatusResult> concurrentRegistrations(List<UUID> ids) throws Exception {
        var executor = Executors.newFixedThreadPool(ids.size());
        CountDownLatch ready = new CountDownLatch(ids.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ArticleViewStatusResult>> futures = new ArrayList<>();
        try {
            for (UUID id : ids) {
                futures.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(10, TimeUnit.SECONDS))
                                        throw new IllegalStateException("시작 대기 시간 초과");
                                    return register(id);
                                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<ArticleViewStatusResult> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void assertCounts(int expected) {
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM article_view_statuses WHERE article_id = ?",
                                Integer.class,
                                articleId))
                .isEqualTo(expected);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT view_count FROM articles WHERE id = ?",
                                Integer.class,
                                articleId))
                .isEqualTo(expected);
    }

    @Nested
    @DisplayName("조회 기록 저장과 시각 갱신")
    class Persistence {

        @Test
        @DisplayName("중복 저장은 기존 기록을 유지하고 최근 조회 시각은 과거로 돌아가지 않는다")
        void givenExistingView_whenInsertAndUpdate_thenPreserveIdentityAndLatestTime() {
            transactionTemplate.executeWithoutResult(
                    status -> {
                        // given
                        UUID userId = userIds.get(0);
                        LocalDateTime first = LocalDateTime.of(2026, 11, 4, 10, 0);
                        assertThat(viewRepository.insertIfAbsent(userId, articleId, first))
                                .isTrue();
                        var original =
                                viewRepository
                                        .findByUserIdAndArticleId(userId, articleId)
                                        .orElseThrow();
                        assertThat(original.getCreatedAt()).isNotNull();
                        // when
                        boolean inserted =
                                viewRepository.insertIfAbsent(
                                        userId, articleId, first.plusHours(1));
                        viewRepository.updateLastViewedAt(userId, articleId, first.plusHours(2));
                        viewRepository.updateLastViewedAt(userId, articleId, first.plusHours(1));
                        var updated =
                                viewRepository
                                        .findByUserIdAndArticleId(userId, articleId)
                                        .orElseThrow();
                        // then
                        assertThat(inserted).isFalse();
                        assertThat(updated.getId()).isEqualTo(original.getId());
                        assertThat(updated.getCreatedAt()).isEqualTo(original.getCreatedAt());
                        assertThat(updated.getLastViewedAt()).isEqualTo(first.plusHours(2));
                    });
        }
    }

    @Nested
    @DisplayName("동시 요청과 트랜잭션")
    class ConcurrencyAndTransaction {

        @Test
        @DisplayName("같은 사용자가 동시에 조회해도 기록 하나와 조회수 1로 집계한다")
        void givenSameUser_whenRegisterConcurrently_thenCountOnlyOnce() throws Exception {
            // given
            UUID userId = userIds.get(0);

            // when
            List<ArticleViewStatusResult> results =
                    concurrentRegistrations(List.of(userId, userId, userId, userId));
            // then
            assertThat(results)
                    .extracting(ArticleViewStatusResult::id)
                    .containsOnly(results.get(0).id());
            assertThat(results)
                    .allSatisfy(result -> assertThat(result.articleViewCount()).isEqualTo(1));
            assertThat(register(userId).id()).isEqualTo(results.get(0).id());
            assertCounts(1);
        }

        @Test
        @DisplayName("서로 다른 사용자가 동시에 조회하면 사용자 수만큼 집계한다")
        void givenDifferentUsers_whenRegisterConcurrently_thenCountEveryUser() throws Exception {
            // given
            List<UUID> requestUserIds = List.copyOf(userIds);

            // when
            List<ArticleViewStatusResult> results = concurrentRegistrations(requestUserIds);

            // then
            assertThat(results).extracting(ArticleViewStatusResult::id).doesNotHaveDuplicates();
            assertCounts(userIds.size());
        }

        @Test
        @DisplayName("후속 처리 실패 시 조회 기록과 조회수 증가를 함께 롤백한다")
        void givenRegistration_whenFollowingOperationFails_thenRollbackRecordAndCount() {
            // given
            UUID userId = userIds.get(0);

            // when & then
            assertThatThrownBy(
                            () ->
                                    transactionTemplate.executeWithoutResult(
                                            status -> {
                                                register(userId);
                                                throw new IllegalStateException("후속 처리 실패");
                                            }))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("후속 처리 실패");
            assertCounts(0);
        }
    }

    @Nested
    @DisplayName("POST /api/articles/{articleId}/article-views")
    class RegisterView {

        @Test
        @DisplayName("재조회하면 ID와 생성 시각과 조회수를 유지하고 최근 조회 시각을 갱신한다")
        void givenExistingView_whenRegisterView_thenRefreshOnlyLastViewedAt() throws Exception {
            // given
            UUID userId = userIds.get(0);
            ArticleViewStatusResult first = register(userId);
            assertThat(first.viewedBy()).isEqualTo(userId);
            assertThat(first.articleId()).isEqualTo(articleId);
            assertThat(first.articleViewCount()).isEqualTo(1);
            assertThat(first.createdAt()).isNotNull();
            assertThat(first.articleTitle()).isEqualTo("기사");
            LocalDateTime old = LocalDateTime.of(2020, 1, 1, 0, 0);
            jdbcTemplate.update(
                    "UPDATE article_view_statuses SET last_viewed_at = ? WHERE id = ?",
                    old,
                    first.id());
            // when
            ResultActions result =
                    mockMvc.perform(
                            post("/api/articles/{id}/article-views", articleId)
                                    .header(USER_HEADER, userId));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(first.id().toString()))
                    .andExpect(jsonPath("$.viewedBy").value(userId.toString()))
                    .andExpect(jsonPath("$.articleId").value(articleId.toString()))
                    .andExpect(jsonPath("$.articleViewCount").value(1));
            transactionTemplate.executeWithoutResult(
                    status -> {
                        var record =
                                viewRepository
                                        .findByUserIdAndArticleId(userId, articleId)
                                        .orElseThrow();
                        assertThat(record.getLastViewedAt()).isAfter(old);
                        assertThat(record.getCreatedAt()).isEqualTo(first.createdAt());
                    });
            assertCounts(1);
        }

        @Test
        @DisplayName("처음 조회하면 200과 새 조회 기록 및 증가한 조회수를 반환한다")
        void givenUnviewedArticle_whenRegisterView_thenReturnRecordAndIncrementCount()
                throws Exception {
            // given
            UUID userId = userIds.get(0);

            // when
            ResultActions result =
                    mockMvc.perform(
                            post("/api/articles/{id}/article-views", articleId)
                                    .header(USER_HEADER, userId));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.articleId").value(articleId.toString()))
                    .andExpect(jsonPath("$.articleViewCount").value(1));
            assertCounts(1);
        }

        @Test
        @DisplayName("없는 기사면 404이며 조회 기록과 조회수를 변경하지 않는다")
        void givenMissingArticle_whenRegisterView_thenReturnNotFound() throws Exception {
            // given
            UUID missingArticleId = UUID.randomUUID();

            // when
            ResultActions result =
                    mockMvc.perform(
                            post("/api/articles/{id}/article-views", missingArticleId)
                                    .header(USER_HEADER, userIds.get(0)));

            // then
            result.andExpect(status().isNotFound());
            assertCounts(0);
        }

        @Test
        @DisplayName("없는 사용자면 404이며 조회 기록과 조회수를 변경하지 않는다")
        void givenMissingUser_whenRegisterView_thenReturnNotFound() throws Exception {
            // given
            UUID missingUserId = UUID.randomUUID();

            // when
            ResultActions result =
                    mockMvc.perform(
                            post("/api/articles/{id}/article-views", articleId)
                                    .header(USER_HEADER, missingUserId));

            // then
            result.andExpect(status().isNotFound());
            assertCounts(0);
        }

        @Test
        @DisplayName("삭제된 기사면 404이며 조회 기록과 조회수를 변경하지 않는다")
        void givenDeletedArticle_whenRegisterView_thenReturnNotFound() throws Exception {
            // given
            transactionTemplate.executeWithoutResult(
                    status -> entityManager.find(Article.class, articleId).softDelete());

            // when
            ResultActions result =
                    mockMvc.perform(
                            post("/api/articles/{id}/article-views", articleId)
                                    .header(USER_HEADER, userIds.get(0)));

            // then
            result.andExpect(status().isNotFound());
            assertCounts(0);
        }

        @Test
        @DisplayName("탈퇴한 사용자면 404이며 조회 기록과 조회수를 변경하지 않는다")
        void givenDeactivatedUser_whenRegisterView_thenReturnNotFound() throws Exception {
            // given
            UUID userId = userIds.get(0);
            transactionTemplate.executeWithoutResult(
                    status -> entityManager.find(User.class, userId).softDelete());
            // when
            ResultActions result =
                    mockMvc.perform(
                            post("/api/articles/{id}/article-views", articleId)
                                    .header(USER_HEADER, userId));

            // then
            result.andExpect(status().isNotFound());
            assertCounts(0);
        }
    }
}
