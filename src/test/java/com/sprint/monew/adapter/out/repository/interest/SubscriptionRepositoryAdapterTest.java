package com.sprint.monew.adapter.out.repository.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class SubscriptionRepositoryAdapterTest {

    @Autowired private SubscriptionRepositoryAdapter repository;
    @Autowired private SubscriptionJpaRepository jpaRepository;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("구독을 저장하고 사용자와 관심사로 조회한다")
    void givenSubscription_whenSave_thenFindByUserAndInterest() {
        User user = savedUser();
        Interest interest = savedInterest("경제");

        Subscription saved = repository.save(Subscription.create(user, interest));
        em.flush();
        em.clear();

        Subscription found =
                repository.findByUserIdAndInterestId(user.getId(), interest.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getInterestId()).isEqualTo(interest.getId());
        assertThat(repository.existsByUserIdAndInterestId(user.getId(), interest.getId())).isTrue();
    }

    @Test
    @DisplayName("구독이 없으면 조회 결과는 비어 있고 존재 여부는 false이다")
    void givenMissingSubscription_whenFind_thenReturnEmpty() {
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        assertThat(repository.findByUserIdAndInterestId(userId, interestId)).isEmpty();
        assertThat(repository.existsByUserIdAndInterestId(userId, interestId)).isFalse();
    }

    @Test
    @DisplayName("요청한 관심사 목록에서 해당 사용자가 구독한 관심사 ID만 조회한다")
    void givenSubscriptions_whenFindSubscribedInterestIds_thenFilterUserAndRequestedInterests() {
        User user = savedUser();
        User other = savedUser();
        Interest selected = savedInterest("경제");
        Interest outside = savedInterest("스포츠");
        Interest otherOnly = savedInterest("기술");
        repository.save(Subscription.create(user, selected));
        repository.save(Subscription.create(user, outside));
        repository.save(Subscription.create(other, otherOnly));

        assertThat(
                        repository.findSubscribedInterestIds(
                                user.getId(), List.of(selected.getId(), otherOnly.getId())))
                .containsExactly(selected.getId());
        assertThat(repository.findSubscribedInterestIds(user.getId(), List.of())).isEmpty();
    }

    @Test
    @DisplayName("관심사의 구독자 조회에서 다른 관심사 구독자와 탈퇴한 사용자를 제외한다")
    void givenActiveAndDeletedSubscribers_whenFindSubscribers_thenReturnOnlyActiveSubscribers() {
        User active = savedUser();
        User deleted = savedUser();
        User other = savedUser();
        Interest target = savedInterest("경제");
        Interest another = savedInterest("스포츠");
        repository.save(Subscription.create(active, target));
        repository.save(Subscription.create(deleted, target));
        repository.save(Subscription.create(other, another));
        deleted.softDelete();
        em.flush();
        em.clear();

        assertThat(repository.findSubscribersByInterestId(target.getId()))
                .containsExactly(active.getId());
        assertThat(repository.findSubscribersByInterestId(UUID.randomUUID())).isEmpty();
        assertThat(repository.findSubscribedInterestIds(deleted.getId(), List.of(target.getId())))
                .isEmpty();
        assertThat(repository.findByUserIdAndInterestId(deleted.getId(), target.getId())).isEmpty();
    }

    @Test
    @DisplayName("특정 구독만 삭제하고 다른 구독은 유지한다")
    void givenSubscriptions_whenDelete_thenRemoveOnlyTargetSubscription() {
        User user = savedUser();
        Interest target = savedInterest("경제");
        Interest retained = savedInterest("스포츠");
        Subscription deleting = repository.save(Subscription.create(user, target));
        repository.save(Subscription.create(user, retained));

        repository.delete(deleting);
        em.flush();
        em.clear();

        assertThat(repository.findByUserIdAndInterestId(user.getId(), target.getId())).isEmpty();
        assertThat(repository.findByUserIdAndInterestId(user.getId(), retained.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("관심사의 모든 구독을 삭제하고 다른 관심사의 구독은 유지한다")
    void givenSubscriptions_whenDeleteAllByInterestId_thenRemoveOnlyTargetInterestSubscriptions() {
        User first = savedUser();
        User second = savedUser();
        Interest target = savedInterest("경제");
        Interest retained = savedInterest("스포츠");
        repository.save(Subscription.create(first, target));
        repository.save(Subscription.create(second, target));
        repository.save(Subscription.create(first, retained));

        repository.deleteAllByInterestId(target.getId());

        assertThat(repository.findSubscribersByInterestId(target.getId())).isEmpty();
        assertThat(repository.findByUserIdAndInterestId(first.getId(), retained.getId()))
                .isPresent();
        assertThat(jpaRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 사용자의 동일 관심사 구독을 저장하고 flush하면 Spring의 무결성 위반 예외가 발생한다")
    void givenDuplicateSubscription_whenSaveAndFlush_thenThrowDataIntegrityViolation() {
        User user = savedUser();
        Interest interest = savedInterest("경제");
        repository.save(Subscription.create(user, interest));
        jpaRepository.flush();

        assertThatThrownBy(
                        () -> {
                            repository.save(Subscription.create(user, interest));
                            jpaRepository.flush();
                        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User savedUser() {
        User user = User.create(UUID.randomUUID() + "@monew.com", "사용자", "password1!");
        em.persist(user);
        return user;
    }

    private Interest savedInterest(String name) {
        Interest interest = Interest.create(name, List.of(name + " 키워드"));
        em.persist(interest);
        return interest;
    }
}
