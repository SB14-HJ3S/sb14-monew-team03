package com.sprint.monew.adapter.out.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class InterestQueryRepositoryImplTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    @Autowired private InterestRepositoryAdapter repository;
    @Autowired private EntityManager em;
    @Autowired private AuditingHandler auditingHandler;

    private List<String> subscriberCountAscendingNames;

    @BeforeEach
    void setUp() {
        Interest alpha = saveInterest("Alpha", 2L, CREATED_AT, "finance", "financial");
        saveInterest("Bravo", 1L, CREATED_AT, "football");
        Interest charlie = saveInterest("Charlie", 2L, CREATED_AT, "technology");
        saveInterest("Delta", 2L, CREATED_AT.plusSeconds(1), "travel");
        saveInterest("Echo", 3L, CREATED_AT, "music");
        em.flush();
        em.clear();

        // 구독자 수와 생성 시각이 같으면 실제 저장된 UUID 순서로 정렬되어야 한다.
        List<String> tiedNames =
                List.of(alpha, charlie).stream()
                        .sorted(Comparator.comparing(interest -> interest.getId().toString()))
                        .map(Interest::getName)
                        .toList();
        subscriberCountAscendingNames =
                List.of("Bravo", tiedNames.get(0), tiedNames.get(1), "Delta", "Echo");
    }

    @AfterEach
    void resetAuditingTime() {
        auditingHandler.setDateTimeProvider(CurrentDateTimeProvider.INSTANCE);
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"NAME, ASC", "NAME, DESC", "SUBSCRIBER_COUNT, ASC", "SUBSCRIBER_COUNT, DESC"})
    @DisplayName("정렬 기준과 방향에 맞게 커서로 모든 페이지를 중복과 누락 없이 조회한다")
    void givenSortAndDirection_whenReadPages_thenRespectOrderAndCursor(
            InterestQuery.SortOrder orderBy, SortDirection direction) {
        List<String> expected =
                new ArrayList<>(
                        orderBy == InterestQuery.SortOrder.NAME
                                ? List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo")
                                : subscriberCountAscendingNames);
        if (direction == SortDirection.DESC) {
            Collections.reverse(expected);
        }
        List<String> collected = new ArrayList<>();
        InterestQuery query = new InterestQuery(null, orderBy, direction, null, null, 2);

        for (int start = 0; start < expected.size(); start += query.limit()) {
            List<Interest> fetched = repository.findByQuery(query);

            // 다음 페이지 판단용으로 limit + 1개를 가져오는 계약도 검증한다.
            assertThat(fetched)
                    .extracting(Interest::getName)
                    .containsExactlyElementsOf(
                            expected.subList(start, Math.min(start + 3, expected.size())));
            assertThat(repository.countByQuery(query)).isEqualTo(5L);
            List<Interest> content = fetched.subList(0, Math.min(query.limit(), fetched.size()));
            collected.addAll(content.stream().map(Interest::getName).toList());
            Interest last = content.get(content.size() - 1);
            query =
                    new InterestQuery(
                            null,
                            orderBy,
                            direction,
                            last.getId().toString(),
                            last.getCreatedAt(),
                            2);
        }

        assertThat(collected).containsExactlyElementsOf(expected);
        assertThat(repository.findByQuery(query)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"ALP, Alpha", "FIN, Alpha", "ball, Bravo"})
    @DisplayName("관심사 이름 또는 키워드를 대소문자 구분 없이 부분 검색하고 중복 없이 센다")
    void givenKeyword_whenFindAndCount_thenMatchNameOrKeywordsWithoutDuplicates(
            String keyword, String expectedName) {
        InterestQuery query =
                new InterestQuery(
                        keyword, InterestQuery.SortOrder.NAME, SortDirection.ASC, null, null, 10);

        assertThat(repository.findByQuery(query))
                .extracting(Interest::getName)
                .containsExactly(expectedName);
        assertThat(repository.countByQuery(query)).isEqualTo(1L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("검색어가 null이거나 비어 있거나 공백뿐이면 모든 관심사를 조회한다")
    void givenBlankKeyword_whenFindAndCount_thenReturnAllInterests(String keyword) {
        InterestQuery query =
                new InterestQuery(
                        keyword, InterestQuery.SortOrder.NAME, SortDirection.ASC, null, null, 10);

        assertThat(repository.findByQuery(query))
                .extracting(Interest::getName)
                .containsExactly("Alpha", "Bravo", "Charlie", "Delta", "Echo");
        assertThat(repository.countByQuery(query)).isEqualTo(5L);
    }

    @Test
    @DisplayName("일치하는 관심사가 없으면 빈 목록과 개수 0을 반환한다")
    void givenUnmatchedKeyword_whenFindAndCount_thenReturnEmptyAndZero() {
        InterestQuery query =
                new InterestQuery(
                        "no-match",
                        InterestQuery.SortOrder.NAME,
                        SortDirection.ASC,
                        null,
                        null,
                        10);

        assertThat(repository.findByQuery(query)).isEmpty();
        assertThat(repository.countByQuery(query)).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("cursor와 after 중 하나만 전달하면 잘못된 요청 예외가 발생한다")
    void givenOnlyOneCursorComponent_whenFind_thenThrowInvalidRequest(boolean cursorOnly) {
        InterestQuery query =
                new InterestQuery(
                        null,
                        InterestQuery.SortOrder.NAME,
                        SortDirection.ASC,
                        cursorOnly ? new UUID(0, 1).toString() : null,
                        cursorOnly ? null : CREATED_AT,
                        2);

        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> repository.findByQuery(query));

        assertThat(thrown.getErrorType()).isEqualTo(CommonErrorType.INVALID_REQUEST);
    }

    @Test
    @DisplayName("cursor가 UUID 형식이 아니면 잘못된 요청 예외가 발생한다")
    void givenMalformedCursor_whenFind_thenThrowInvalidRequest() {
        InterestQuery query =
                new InterestQuery(
                        null,
                        InterestQuery.SortOrder.NAME,
                        SortDirection.ASC,
                        "invalid-uuid",
                        CREATED_AT,
                        2);

        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> repository.findByQuery(query));

        assertThat(thrown.getErrorType()).isEqualTo(CommonErrorType.INVALID_REQUEST);
        assertThat(thrown).hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("커서에 해당하는 관심사가 없으면 관심사 없음 예외가 발생한다")
    void givenMissingCursorInterest_whenFind_thenThrowInterestNotFound() {
        InterestQuery query =
                new InterestQuery(
                        null,
                        InterestQuery.SortOrder.NAME,
                        SortDirection.ASC,
                        new UUID(0, 999).toString(),
                        CREATED_AT,
                        2);

        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> repository.findByQuery(query));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INTEREST_NOT_FOUND);
    }

    private Interest saveInterest(String name, long count, Instant createdAt, String... keywords) {
        Interest interest = Interest.create(name, Arrays.asList(keywords));
        ReflectionTestUtils.setField(interest, "subscriberCount", count);
        auditingHandler.setDateTimeProvider(() -> Optional.of(createdAt));
        em.persist(interest);
        return interest;
    }
}
