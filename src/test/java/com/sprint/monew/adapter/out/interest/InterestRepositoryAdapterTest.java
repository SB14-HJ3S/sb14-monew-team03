package com.sprint.monew.adapter.out.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class InterestRepositoryAdapterTest {

    @Autowired private InterestRepositoryAdapter repository;
    @Autowired private InterestJpaRepository jpaRepository;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("관심사를 저장하면 키워드도 저장되고 ID와 생성 시각으로 다시 조회할 수 있다")
    void givenInterest_whenSave_thenPersistInterestAndKeywords() {
        Interest saved = repository.save(Interest.create("경제", List.of("금융", "주식")));
        em.flush();
        em.clear();

        Interest found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getName()).isEqualTo("경제");
        assertThat(found.getSubscriberCount()).isZero();
        assertThat(found.getKeywordNames()).containsExactlyInAnyOrder("금융", "주식");
        assertThat(repository.existsById(saved.getId())).isTrue();
    }

    @Test
    @DisplayName("없는 관심사는 조회되지 않고 존재 여부도 false이다")
    void givenMissingInterest_whenFindById_thenReturnEmpty() {
        UUID id = UUID.randomUUID();

        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    @DisplayName("모든 관심사 이름을 조회한다")
    void givenInterests_whenFindAllNames_thenReturnAllNames() {
        repository.save(Interest.create("경제", List.of("금융")));
        repository.save(Interest.create("스포츠", List.of("축구")));

        assertThat(repository.findAllNames()).containsExactlyInAnyOrder("경제", "스포츠");
    }

    @Test
    @DisplayName("키워드 포함 조회는 키워드를 즉시 로딩하고 없는 관심사는 빈 Optional을 반환한다")
    void givenInterest_whenFindByIdWithKeywords_thenFetchKeywords() {
        Interest saved = repository.save(Interest.create("경제", List.of("금융", "주식")));
        em.flush();
        em.clear();

        Interest found = repository.findByIdWithKeywords(saved.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getKeywords())).isTrue();
        assertThat(found.getKeywordNames()).containsExactlyInAnyOrder("금융", "주식");
        assertThat(repository.findByIdWithKeywords(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("관심사를 삭제하면 연결된 키워드도 삭제하고 다른 관심사는 유지한다")
    void givenInterests_whenDelete_thenRemoveOnlyTargetAndItsKeywords() {
        Interest target = repository.save(Interest.create("경제", List.of("금융", "주식")));
        Interest retained = repository.save(Interest.create("스포츠", List.of("축구")));
        em.flush();

        repository.delete(target);
        em.flush();
        em.clear();

        assertThat(repository.findById(target.getId())).isEmpty();
        assertThat(
                        em.createQuery(
                                        "select count(k) from InterestKeyword k where k.interest.id = :interestId",
                                        Long.class)
                                .setParameter("interestId", target.getId())
                                .getSingleResult())
                .isZero();
        assertThat(
                        repository
                                .findByIdWithKeywords(retained.getId())
                                .orElseThrow()
                                .getKeywordNames())
                .containsExactly("축구");
    }

    @Test
    @DisplayName("동일한 이름의 관심사를 저장하고 flush하면 Spring의 무결성 위반 예외가 발생한다")
    void givenDuplicateName_whenSaveAndFlush_thenThrowDataIntegrityViolation() {
        repository.save(Interest.create("경제", List.of("금융")));
        jpaRepository.flush();

        assertThatThrownBy(
                        () -> {
                            repository.save(Interest.create("경제", List.of("주식")));
                            jpaRepository.flush();
                        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("구독자 수를 증가시키면 DB에 반영한다")
    void givenInterest_whenIncreaseSubscriberCount_thenIncrementInDatabase() {
        Interest saved = repository.save(Interest.create("경제", List.of("금융")));

        repository.increaseSubscriberCount(saved.getId());

        assertThat(repository.findById(saved.getId()).orElseThrow().getSubscriberCount())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("구독자 수를 감소시키되 0 아래로 내려가지 않는다")
    void givenInterest_whenDecreaseSubscriberCount_thenDecrementAndKeepNonNegative() {
        Interest saved = repository.save(Interest.create("경제", List.of("금융")));
        repository.increaseSubscriberCount(saved.getId());

        repository.decreaseSubscriberCount(saved.getId());
        assertThat(repository.findById(saved.getId()).orElseThrow().getSubscriberCount()).isZero();
        repository.decreaseSubscriberCount(saved.getId());
        assertThat(repository.findById(saved.getId()).orElseThrow().getSubscriberCount()).isZero();
    }

    @Test
    @DisplayName("삭제 대상 사용자들의 구독 수만큼 관심사별 구독자 수를 줄이고 다른 관심사는 유지한다")
    void givenUsersSharingInterests_whenDecreaseCounts_thenSubtractOnlyTheirSubscriptions() {
        Interest shared = repository.save(Interest.create("경제", List.of("금융")));
        Interest targetOnly = repository.save(Interest.create("스포츠", List.of("축구")));
        Interest untouched = repository.save(Interest.create("기술", List.of("AI")));
        User first = savedUser();
        User second = savedUser();
        User remaining = savedUser();
        em.persist(Subscription.create(first, shared));
        em.persist(Subscription.create(second, shared));
        em.persist(Subscription.create(remaining, shared));
        em.persist(Subscription.create(first, targetOnly));
        em.persist(Subscription.create(remaining, untouched));
        ReflectionTestUtils.setField(shared, "subscriberCount", 3L);
        ReflectionTestUtils.setField(targetOnly, "subscriberCount", 1L);
        ReflectionTestUtils.setField(untouched, "subscriberCount", 1L);
        em.flush();
        em.clear();

        repository.decreaseSubscriberCountsByUserIds(List.of(first.getId(), second.getId()));

        assertThat(repository.findById(shared.getId()).orElseThrow().getSubscriberCount())
                .isEqualTo(1L);
        assertThat(repository.findById(targetOnly.getId()).orElseThrow().getSubscriberCount())
                .isZero();
        assertThat(repository.findById(untouched.getId()).orElseThrow().getSubscriberCount())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("구독자 수가 실제 구독보다 작아도 사용자별 일괄 감소 후 음수가 되지 않는다")
    void givenUndercountedInterest_whenDecreaseCounts_thenKeepZero() {
        Interest saved = repository.save(Interest.create("경제", List.of("금융")));
        User first = savedUser();
        User second = savedUser();
        em.persist(Subscription.create(first, saved));
        em.persist(Subscription.create(second, saved));
        ReflectionTestUtils.setField(saved, "subscriberCount", 1L);
        em.flush();
        em.clear();

        repository.decreaseSubscriberCountsByUserIds(List.of(first.getId(), second.getId()));

        assertThat(repository.findById(saved.getId()).orElseThrow().getSubscriberCount()).isZero();
    }

    @Test
    @DisplayName("구독 중인 관심사만 중복 없이 조회하고 키워드를 즉시 로딩한다")
    void givenSubscribedAndUnsubscribedInterests_whenFindSubscribed_thenFetchOnlySubscribed() {
        Interest subscribed = repository.save(Interest.create("경제", List.of("금융", "주식")));
        repository.save(Interest.create("스포츠", List.of("축구")));
        repository.increaseSubscriberCount(subscribed.getId());

        List<Interest> result = repository.findSubscribedWithKeywords();

        assertThat(result).extracting(Interest::getId).containsExactly(subscribed.getId());
        assertThat(Hibernate.isInitialized(result.get(0).getKeywords())).isTrue();
        assertThat(result.get(0).getKeywordNames()).containsExactlyInAnyOrder("금융", "주식");
    }

    private User savedUser() {
        User user = User.create(UUID.randomUUID() + "@monew.com", "사용자", "password1!");
        em.persist(user);
        return user;
    }
}
