package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
@DisplayName("기사 목록 조회 통합 테스트")
class ArticleListIntegrationTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 10, 22, 9, 0);
    private static final Instant CREATED = Instant.parse("2026-10-22T00:00:00Z");

    @Autowired private ArticleUseCase articleUseCase;
    @Autowired private EntityManager em;

    private UUID userId;
    private UUID otherUserId;
    private UUID interestId;
    private Article a;
    private Article b;
    private Article c;
    private Article d;
    private Article e;

    @AfterEach
    void tearDown() {
        RequestIdContext.clear();
    }

    @BeforeEach
    void setUp() {
        User user = User.create("reader@example.com", "독자", "password1!");
        User other = User.create("other@example.com", "다른 독자", "password1!");
        em.persist(user);
        em.persist(other);
        userId = user.getId();
        otherUserId = other.getId();

        RequestIdContext.set(userId);

        Interest economy = Interest.create("경제", List.of("경제"));
        Interest tech = Interest.create("기술", List.of("AI"));
        em.persist(economy);
        em.persist(tech);
        interestId = economy.getId();

        a = save("A 경제", "금리", ArticleSourceType.NAVER, DATE.minusDays(2), 10, 1, 1);
        b = save("B 산업", "경제 성장", ArticleSourceType.HANKYUNG, DATE.minusDays(1), 20, 3, 2);
        c = save("C 스포츠", "축구", ArticleSourceType.CHOSUN, DATE, 30, 2, 3);
        d = save("D 기술", "AI 소식", ArticleSourceType.YEONHAP, DATE.plusDays(1), 40, 5, 4);
        e = save("E 경제", "주식", ArticleSourceType.NAVER, DATE.plusDays(1), 40, 5, 5);
        Article deleted =
                save("F 경제 삭제", "경제", ArticleSourceType.NAVER, DATE.plusDays(2), 100, 10, 6);
        deleted.softDelete();

        for (Article item : List.of(a, b, e, deleted)) {
            em.persist(ArticleInterest.create(item, economy));
        }
        // 한 기사가 여러 관심사에 속해도 결과와 개수가 중복되지 않아야 합니다.
        em.persist(ArticleInterest.create(a, tech));
        em.persist(ArticleViewStatus.create(user, a, DATE));
        em.persist(ArticleViewStatus.create(user, e, DATE));
        em.persist(ArticleViewStatus.create(other, d, DATE));
        em.flush();
        em.clear();
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @CsvSource({
        "PUBLISH_DATE, ASC, ABCDE",
        "PUBLISH_DATE, DESC, EDCBA",
        "COMMENT_COUNT, ASC, ACBDE",
        "COMMENT_COUNT, DESC, EDBCA",
        "VIEW_COUNT, ASC, ABCDE",
        "VIEW_COUNT, DESC, EDCBA"
    })
    @DisplayName("모든 정렬 기준과 방향으로 조회하고 삭제 기사는 제외한다")
    void sorts(ArticleQuery.SortOrder order, SortDirection direction, String expected) {
        PageResult<ArticleResult> page =
                articleUseCase.findArticlePage(query(order, direction, 10, null, null));
        assertThat(labels(page.content())).isEqualTo(expected);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.nextAfter()).isNull();
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "PUBLISH_DATE, ASC, ABCDE",
        "PUBLISH_DATE, DESC, EDCBA",
        "COMMENT_COUNT, ASC, ACBDE",
        "COMMENT_COUNT, DESC, EDBCA",
        "VIEW_COUNT, ASC, ABCDE",
        "VIEW_COUNT, DESC, EDCBA"
    })
    @DisplayName("동일 정렬값은 생성 시각으로 구분하며 모든 페이지를 중복 없이 조회한다")
    void paginates(ArticleQuery.SortOrder order, SortDirection direction, String expected) {
        List<ArticleResult> all = new ArrayList<>();
        String cursor = null;
        Instant after = null;
        // limit=1로 D와 E의 동일 정렬값이 반드시 페이지 경계에 걸리게 합니다.
        for (int i = 0; i < 5; i++) {
            PageResult<ArticleResult> page =
                    articleUseCase.findArticlePage(query(order, direction, 1, cursor, after));
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(5);
            assertThat(page.size()).isEqualTo(1);
            all.addAll(page.content());
            if (i < 4) {
                assertThat(page.hasNext()).isTrue();
                assertThat(page.nextCursor()).isNotBlank();
                assertThat(page.nextAfter()).isNotNull();
            } else {
                assertThat(page.hasNext()).isFalse();
                assertThat(page.nextCursor()).isNull();
                assertThat(page.nextAfter()).isNull();
            }
            cursor = page.nextCursor();
            after = page.nextAfter();
        }
        assertThat(labels(all)).isEqualTo(expected);
        assertThat(all).extracting(ArticleResult::id).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("응답 마지막 기사의 값으로 커서를 만들고 추가 조회한 기사는 제외한다")
    void nextCursorUsesLastReturnedArticle() {
        PageResult<ArticleResult> page =
                articleUseCase.findArticlePage(
                        query(
                                ArticleQuery.SortOrder.COMMENT_COUNT,
                                SortDirection.DESC,
                                2,
                                null,
                                null));
        assertThat(labels(page.content())).isEqualTo("ED");
        assertThat(page.nextCursor()).isEqualTo("5");
        assertThat(page.nextAfter()).isEqualTo(CREATED.plusSeconds(4));
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("제목 또는 요약에 검색어가 포함된 기사만 조회한다")
    void searchesTitleOrSummary() {
        PageResult<ArticleResult> page = search("경제", null, null, null, null);
        assertThat(labels(page.content())).isEqualTo("EBA");
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("출처 목록은 OR로, 날짜 경계는 양 끝을 포함해 적용한다")
    void filtersSourcesAndDateBoundaries() {
        PageResult<ArticleResult> page =
                search(
                        null,
                        null,
                        List.of(ArticleSourceType.NAVER, ArticleSourceType.HANKYUNG),
                        DATE.minusDays(2),
                        DATE.minusDays(1));
        assertThat(labels(page.content())).isEqualTo("BA");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("관심사 연결은 중복 없이 조회하고 다른 필터와 AND로 결합한다")
    void combinesInterestAndFilters() {
        assertThat(search(null, interestId, null, null, null).totalElements()).isEqualTo(3);
        PageResult<ArticleResult> page =
                search("경제", interestId, List.of(ArticleSourceType.NAVER), DATE, DATE.plusDays(1));
        assertThat(labels(page.content())).isEqualTo("E");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 페이지와 null 커서를 반환한다")
    void emptyResult() {
        PageResult<ArticleResult> page = search("없는검색어", null, null, null, null);
        assertThat(page.content()).isEmpty();
        assertThat(page.size()).isZero();
        assertThat(page.totalElements()).isZero();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.nextAfter()).isNull();
    }

    @Test
    @DisplayName("빈 검색어와 빈 출처 목록은 필터를 적용하지 않는다")
    void emptyFilters() {
        assertThat(search(" ", null, List.of(), null, null).totalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("요청자별 열람 기록만 반영한다")
    void viewedByMeIsUserSpecific() {
        PageResult<ArticleResult> first =
                articleUseCase.findArticlePage(
                        query(
                                ArticleQuery.SortOrder.PUBLISH_DATE,
                                SortDirection.DESC,
                                10,
                                null,
                                null));
        assertThat(
                        first.content().stream()
                                .filter(ArticleResult::viewedByMe)
                                .map(ArticleResult::id))
                .containsExactly(e.getId(), a.getId());
        RequestIdContext.set(otherUserId);
        PageResult<ArticleResult> second =
                articleUseCase.findArticlePage(
                        query(
                                ArticleQuery.SortOrder.PUBLISH_DATE,
                                SortDirection.DESC,
                                10,
                                null,
                                null));
        assertThat(
                        second.content().stream()
                                .filter(ArticleResult::viewedByMe)
                                .map(ArticleResult::id))
                .containsExactly(d.getId());
    }

    private ArticleQuery query(
            ArticleQuery.SortOrder order,
            SortDirection direction,
            int limit,
            String cursor,
            Instant after) {
        return new ArticleQuery(
                null, null, null, null, null, order, direction, cursor, after, limit);
    }

    private PageResult<ArticleResult> search(
            String keyword,
            UUID interest,
            List<ArticleSourceType> sources,
            LocalDateTime from,
            LocalDateTime to) {
        return articleUseCase.findArticlePage(
                new ArticleQuery(
                        keyword,
                        interest,
                        sources,
                        from,
                        to,
                        ArticleQuery.SortOrder.PUBLISH_DATE,
                        SortDirection.DESC,
                        null,
                        null,
                        10));
    }

    private String labels(List<ArticleResult> articles) {
        return articles.stream()
                .map(item -> item.title().substring(0, 1))
                .reduce("", String::concat);
    }

    private Article save(
            String title,
            String summary,
            ArticleSourceType source,
            LocalDateTime publishedAt,
            int views,
            int comments,
            int second) {
        Article entity =
                Article.create(
                        source,
                        "https://example.com/" + UUID.randomUUID(),
                        title,
                        publishedAt,
                        summary);
        em.persist(entity);
        em.flush();
        // Auditing 시간에 의존하지 않고 DB에 고정 시각과 정렬용 카운터를 설정합니다.
        em.createNativeQuery(
                        """
                    update articles
                    set created_at = ?1,
                        view_count = ?2,
                        comment_count = ?3
                    where id = ?4
                    """)
                .setParameter(1, CREATED.plusSeconds(second))
                .setParameter(2, views)
                .setParameter(3, comments)
                .setParameter(4, entity.getId())
                .executeUpdate();

        em.refresh(entity);
        return entity;
    }
}
