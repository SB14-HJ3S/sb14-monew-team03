package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.repository.interest.InterestJpaRepository;
import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @Transactional을 붙이면 커밋이 측정 밖으로 밀리므로 사용하지 않는다.
 */
@IntegrationTest
@Tag("performance")
@Slf4j
@TestPropertySource(
        properties = {
            "spring.jpa.show-sql=false",
            "logging.level.root=WARN",
            "logging.level.com.sprint.monew=WARN",
            "logging.level.com.sprint.monew.application.article.ArticleCollectionBenchmarkTest=INFO",
            "logging.level.org.hibernate.SQL=OFF",
            "logging.level.org.hibernate.orm.jdbc.bind=OFF"
        })
class ArticleCollectionBenchmarkTest {

    private static final int TOTAL_ARTICLES = 100_000;
    private static final int ARTICLES_PER_INTEREST = 5_000;
    private static final int INTEREST_COUNT = TOTAL_ARTICLES / ARTICLES_PER_INTEREST;
    private static final String URL_PREFIX = "https://benchmark.example.com/articles/";
    private static final String WARMUP_PREFIX = "https://benchmark.example.com/warmup/";
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 11, 6, 12, 0);

    @Autowired private ArticleCollectionUseCase collectionUseCase;
    @Autowired private InterestJpaRepository interests;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory entityManagerFactory;

    // 기사·연결 적재만 측정한다. 비동기 알림 및 다른 이벤트 후속 처리는 실행하지 않는다.
    @MockitoBean private ArticleEventPublisherPort eventPublisher;

    @Test
    @DisplayName("신규 기사 10만 건과 관심사 연결 10만 건을 커밋 완료까지 측정한다")
    void benchmarkNewArticles() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(countArticles()).isZero();
        assertThat(countLinks()).isZero();

        List<Interest> targets = new ArrayList<>(INTEREST_COUNT);
        for (int i = 0; i < INTEREST_COUNT; i++) {
            targets.add(interests.saveAndFlush(Interest.create("적재성능측정-" + i, List.of("AI"))));
        }

        // DTO 생성은 타이머 밖에서 완료한다. 관심사별 기사 URL은 서로 겹치지 않는다.
        List<List<CollectedArticle>> inputs = new ArrayList<>(INTEREST_COUNT);
        for (int i = 0; i < INTEREST_COUNT; i++) {
            inputs.add(
                    createArticles(URL_PREFIX, i * ARTICLES_PER_INTEREST, ARTICLES_PER_INTEREST));
        }

        // 동일 저장 경로를 워밍업한 뒤 데이터를 지운다. 테이블/인덱스 공간은 재사용될 수 있다.
        int warmupSaved =
                collectionUseCase.saveArticles(
                        targets.get(0), createArticles(WARMUP_PREFIX, 0, 1_000));
        assertThat(warmupSaved).isEqualTo(1_000);
        jdbc.update(
                "delete from article_interests where article_id in"
                        + " (select id from articles where source_url like ?)",
                WARMUP_PREFIX + "%");
        jdbc.update("delete from articles where source_url like ?", WARMUP_PREFIX + "%");
        assertThat(countArticles()).isZero();
        assertThat(countLinks()).isZero();

        Map<String, Object> properties = entityManagerFactory.getProperties();
        log.info(
                String.format(
                        Locale.ROOT,
                        "BENCHMARK CONFIG: articles=%,d, interests=%d, articlesPerTransaction=%,d,"
                                + " batch_size=%s, order_inserts=%s",
                        TOTAL_ARTICLES,
                        INTEREST_COUNT,
                        ARTICLES_PER_INTEREST,
                        properties.getOrDefault("hibernate.jdbc.batch_size", "default (0)"),
                        properties.getOrDefault("hibernate.order_inserts", "default (false)")));

        long[] chunkNanos = new long[INTEREST_COUNT];
        int saved = 0;
        long started = System.nanoTime();
        for (int i = 0; i < INTEREST_COUNT; i++) {
            long chunkStarted = System.nanoTime();
            // Spring 트랜잭션 프록시가 커밋을 완료한 후 반환한다.
            saved += collectionUseCase.saveArticles(targets.get(i), inputs.get(i));
            chunkNanos[i] = System.nanoTime() - chunkStarted;
        }
        long elapsedNanos = System.nanoTime() - started;

        // 건수 검증 SELECT와 결과 출력은 측정에서 제외한다.
        long articleRows = countArticles();
        long linkRows = countLinks();
        assertThat(saved).isEqualTo(TOTAL_ARTICLES);
        assertThat(articleRows).isEqualTo(TOTAL_ARTICLES);
        assertThat(linkRows).isEqualTo(TOTAL_ARTICLES);

        double seconds = elapsedNanos / 1_000_000_000.0;
        for (int i = 0; i < INTEREST_COUNT; i++) {
            log.info(
                    String.format(
                            Locale.ROOT,
                            "BENCHMARK CHUNK: %02d/%02d, articles=%d, seconds=%.3f",
                            i + 1,
                            INTEREST_COUNT,
                            ARTICLES_PER_INTEREST,
                            chunkNanos[i] / 1_000_000_000.0));
        }
        log.info(
                String.format(
                        Locale.ROOT,
                        "BENCHMARK RESULT: articles=%d, links=%d, transactions=%d, seconds=%.3f,"
                                + " articlesPerSecond=%.1f, rowsPerSecond=%.1f",
                        articleRows,
                        linkRows,
                        INTEREST_COUNT,
                        seconds,
                        TOTAL_ARTICLES / seconds,
                        (articleRows + linkRows) / seconds));
    }

    private List<CollectedArticle> createArticles(String prefix, int start, int count) {
        List<CollectedArticle> articles = new ArrayList<>(count);
        for (int i = start; i < start + count; i++) {
            articles.add(
                    new CollectedArticle(
                            ArticleSourceType.NAVER,
                            prefix + i,
                            "AI 산업과 기술 동향을 다루는 기사 " + i,
                            PUBLISHED_AT,
                            "인공지능 산업의 서비스 도입과 기술 개발 동향을 소개합니다. 기업의 투자와 연구 성과, 시장 변화와 활용 사례를 함께 살펴봅니다. "
                                            .repeat(3)
                                    + i));
        }
        return articles;
    }

    private long countArticles() {
        return jdbc.queryForObject("select count(*) from articles", Long.class);
    }

    private long countLinks() {
        return jdbc.queryForObject("select count(*) from article_interests", Long.class);
    }
}
