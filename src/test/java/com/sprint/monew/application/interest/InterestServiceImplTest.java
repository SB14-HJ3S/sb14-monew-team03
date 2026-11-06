package com.sprint.monew.application.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.required.InterestRepository;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.interest.entity.InterestKeyword;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceImplTest {

    @Mock InterestRepository repository;

    @InjectMocks InterestServiceImpl service;

    @Test
    @DisplayName("Interest 이름의 유사도가 80 미만인 경우 관심사를 등록할 수 있다.")
    void givenNotSimilarName_whenCreate_thenSaveAndReturnInterest() {
        // given
        String notSimilarName = "박수민";
        List<String> keywords = List.of("1", "2", "3");
        Interest creating = Interest.create(notSimilarName, keywords);

        when(repository.findAllNames()).thenReturn(List.of("유영우"));
        when(repository.save(any(Interest.class))).thenReturn(creating);

        // when
        Interest created = service.create(notSimilarName, keywords);

        // then
        assertThat(created).isEqualTo(creating);

        verify(repository).findAllNames();
        verify(repository)
                .save(
                        argThat(
                                interest ->
                                        interest.getName().equals(notSimilarName)
                                                && interest.getKeywords().stream()
                                                        .map(InterestKeyword::getKeyword)
                                                        .toList()
                                                        .containsAll(keywords)));
    }

    @Test
    @DisplayName("Interest 이름의 유사도가 80 이상인 경우 예외를 던진다.")
    void givenSimilarName_whenCreate_thenThrowException() {
        // given
        String similarName = "박수민1";
        List<String> keywords = List.of("1", "2", "3");

        when(repository.findAllNames()).thenReturn(List.of("박수민2"));

        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class, () -> service.create(similarName, keywords));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.SIMILAR_NAME_CONFLICT);

        verify(repository).findAllNames();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("이름 유사도가 정확히 80%이면 관심사를 등록하지 않는다.")
    void givenSimilarityAtThreshold_whenCreate_thenThrowException() {
        // given
        when(repository.findAllNames()).thenReturn(List.of("abcde"));

        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class, () -> service.create("abcdx", List.of("키워드")));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.SIMILAR_NAME_CONFLICT);
        assertThat(thrown.getDetails())
                .containsEntry("requestedName", "abcdx")
                .containsEntry("existingName", "abcde")
                .containsEntry("similarity", 0.8D);
        verify(repository).findAllNames();
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @EnumSource(InterestQuery.SortOrder.class)
    @DisplayName("다음 페이지가 있으면 limit만큼 반환하고 마지막 관심사의 ID와 생성 시각을 커서로 사용한다.")
    void givenMoreThanLimit_whenFindPageByQuery_thenReturnLimitedContentAndNextCursor(
            InterestQuery.SortOrder orderBy) {
        // given
        InterestQuery query = new InterestQuery("경제", orderBy, SortDirection.DESC, null, null, 2);
        Interest first = Interest.create("경제", List.of("금융"));
        Interest last = mock(Interest.class);
        Interest extra = Interest.create("세계 경제", List.of("환율"));
        UUID lastId = UUID.randomUUID();
        Instant lastCreatedAt = Instant.parse("2026-11-01T00:00:00Z");
        when(last.getId()).thenReturn(lastId);
        when(last.getCreatedAt()).thenReturn(lastCreatedAt);
        when(repository.findByQuery(query)).thenReturn(List.of(first, last, extra));
        when(repository.countByQuery(query)).thenReturn(7L);

        // when
        PageResult<Interest> result = service.findPageByQuery(query);

        // then
        assertThat(result.content()).containsExactly(first, last);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(7L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(lastId.toString());
        assertThat(result.nextAfter()).isEqualTo(lastCreatedAt);
        verify(repository).findByQuery(query);
        verify(repository).countByQuery(query);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("조회 결과가 limit과 같으면 전체 결과를 반환하고 다음 페이지 커서를 만들지 않는다.")
    void givenExactlyLimit_whenFindPageByQuery_thenReturnLastPageWithoutCursor() {
        // given
        String cursor = UUID.randomUUID().toString();
        Instant after = Instant.parse("2026-11-01T00:00:00Z");
        InterestQuery query =
                new InterestQuery(
                        null, InterestQuery.SortOrder.NAME, SortDirection.ASC, cursor, after, 2);
        Interest first = Interest.create("경제", List.of("금융"));
        Interest second = Interest.create("스포츠", List.of("축구"));
        List<Interest> interests = List.of(first, second);
        when(repository.findByQuery(query)).thenReturn(interests);
        when(repository.countByQuery(query)).thenReturn(5L);

        // when
        PageResult<Interest> result = service.findPageByQuery(query);

        // then
        assertThat(result).isEqualTo(new PageResult<>(interests, null, null, 2, 5L, false));
        verify(repository).findByQuery(query);
        verify(repository).countByQuery(query);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("조회된 관심사가 없으면 빈 페이지를 반환한다.")
    void givenNoInterests_whenFindPageByQuery_thenReturnEmptyPage() {
        // given
        InterestQuery query =
                new InterestQuery(
                        "없는 관심사", InterestQuery.SortOrder.NAME, SortDirection.ASC, null, null, 10);
        when(repository.findByQuery(query)).thenReturn(List.of());
        when(repository.countByQuery(query)).thenReturn(0L);

        // when
        PageResult<Interest> result = service.findPageByQuery(query);

        // then
        assertThat(result).isEqualTo(new PageResult<>(List.of(), null, null, 0, 0L, false));
        verify(repository).findByQuery(query);
        verify(repository).countByQuery(query);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("구독 중인 관심사를 키워드와 함께 조회해 결과를 반환한다.")
    void givenSubscribedInterests_whenFindSubscribedWithKeywords_thenReturnRepositoryResult() {
        // given
        List<Interest> interests =
                List.of(
                        Interest.create("경제", List.of("금융", "주식")),
                        Interest.create("스포츠", List.of("축구")));
        when(repository.findSubscribedWithKeywords()).thenReturn(interests);

        // when
        List<Interest> result = service.findSubscribedWithKeywords();

        // then
        assertThat(result).containsExactlyElementsOf(interests);
        assertThat(result.get(0).getKeywordNames()).containsExactly("금융", "주식");
        assertThat(result.get(1).getKeywordNames()).containsExactly("축구");
        verify(repository).findSubscribedWithKeywords();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("구독 중인 관심사가 없으면 빈 목록을 반환한다.")
    void givenNoSubscribedInterests_whenFindSubscribedWithKeywords_thenReturnEmptyList() {
        // given
        when(repository.findSubscribedWithKeywords()).thenReturn(List.of());

        // when
        List<Interest> result = service.findSubscribedWithKeywords();

        // then
        assertThat(result).isEmpty();
        verify(repository).findSubscribedWithKeywords();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("존재하는 관심사의 키워드를 수정하고 수정된 관심사를 반환한다.")
    void givenExistingInterest_whenUpdate_thenUpdateKeywordsAndReturnInterest() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest interest = Interest.create("관심사", List.of("기존", "유지"));
        List<String> newKeywords = List.of("유지", "추가");

        when(repository.findByIdWithKeywords(interestId)).thenReturn(Optional.of(interest));

        // when
        Interest updated = service.update(interestId, newKeywords);

        // then
        assertThat(updated).isSameAs(interest);
        assertThat(updated.getKeywords())
                .extracting(InterestKeyword::getKeyword)
                .containsExactly("유지", "추가");

        verify(repository).findByIdWithKeywords(interestId);
    }

    @Test
    @DisplayName("존재하지 않는 관심사의 키워드를 수정하면 예외를 던진다.")
    void givenNonExistingInterest_whenUpdate_thenThrowException() {
        // given
        UUID interestId = UUID.randomUUID();

        when(repository.findByIdWithKeywords(interestId)).thenReturn(Optional.empty());

        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class, () -> service.update(interestId, List.of("새 키워드")));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INTEREST_NOT_FOUND);

        verify(repository).findByIdWithKeywords(interestId);
    }

    @Test
    @DisplayName("존재하는 관심사를 ID로 조회한다.")
    void givenExistingInterest_whenGetById_thenReturnInterest() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest interest = Interest.create("관심사", List.of("키워드"));

        when(repository.findById(interestId)).thenReturn(Optional.of(interest));

        // when
        Interest found = service.getById(interestId);

        // then
        assertThat(found).isSameAs(interest);
        verify(repository).findById(interestId);
    }

    @Test
    @DisplayName("관심사를 삭제한다.")
    void givenInterest_whenDelete_thenDeleteInterest() {
        // given
        Interest interest = Interest.create("관심사", List.of("키워드"));

        // when
        service.delete(interest);

        // then
        verify(repository).delete(interest);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 ID로 조회하면 예외를 던진다.")
    void givenNonExistingInterest_whenGetById_thenThrowException() {
        // given
        UUID interestId = UUID.randomUUID();

        when(repository.findById(interestId)).thenReturn(Optional.empty());

        // when
        MonewException thrown =
                catchThrowableOfType(MonewException.class, () -> service.getById(interestId));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INTEREST_NOT_FOUND);
        assertThat(thrown.getDetails()).containsEntry("id", interestId);

        verify(repository).findById(interestId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("관심사의 구독자 수를 증가시키고 변경된 관심사를 반환한다.")
    void givenExistingInterest_whenIncreaseSubscriberCount_thenUpdateAndReturnInterest() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest interest = Interest.create("관심사", List.of("키워드"));
        when(repository.findById(interestId)).thenReturn(Optional.of(interest));

        // when
        Interest result = service.increaseSubscriberCount(interestId);

        // then
        assertThat(result).isSameAs(interest);
        InOrder inOrder = inOrder(repository);
        inOrder.verify(repository).increaseSubscriberCount(interestId);
        inOrder.verify(repository).findById(interestId);
    }

    @Test
    @DisplayName("존재하는 관심사의 구독자 수를 감소시킨다.")
    void givenExistingInterest_whenDecreaseSubscriberCount_thenUpdateCount() {
        // given
        UUID interestId = UUID.randomUUID();
        when(repository.existsById(interestId)).thenReturn(true);

        // when
        service.decreaseSubscriberCount(interestId);

        // then
        InOrder inOrder = inOrder(repository);
        inOrder.verify(repository).existsById(interestId);
        inOrder.verify(repository).decreaseSubscriberCount(interestId);
    }

    @Test
    @DisplayName("존재하지 않는 관심사의 구독자 수를 감소시키면 예외가 발생한다.")
    void givenNonExistingInterest_whenDecreaseSubscriberCount_thenThrowException() {
        // given
        UUID interestId = UUID.randomUUID();
        when(repository.existsById(interestId)).thenReturn(false);

        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class, () -> service.decreaseSubscriberCount(interestId));

        // then
        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.INTEREST_NOT_FOUND);
        assertThat(thrown.getDetails()).containsEntry("id", interestId);
        verify(repository, never()).decreaseSubscriberCount(interestId);
    }

    @Test
    @DisplayName("사용자 목록에 대한 관심사별 구독자 수 정리를 저장소에 한 번 요청한다.")
    void givenUserIds_whenDecreaseSubscriberCounts_thenDelegateToRepositoryOnce() {
        // given
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        // when
        service.decreaseSubscriberCountsByUserIds(userIds);

        // then
        verify(repository).decreaseSubscriberCountsByUserIds(userIds);
        verifyNoMoreInteractions(repository);
    }
}
