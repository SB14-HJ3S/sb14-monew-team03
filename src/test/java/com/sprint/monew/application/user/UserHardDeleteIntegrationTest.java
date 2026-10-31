package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.adapter.out.interest.InterestRepositoryAdapter;
import com.sprint.monew.adapter.out.interest.SubscriptionRepositoryAdapter;
import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import com.sprint.monew.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UserHardDeleteIntegrationTest {

    @Autowired private UserUseCase userUseCase;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private InterestRepositoryAdapter interestRepository;
    @Autowired private SubscriptionRepositoryAdapter subscriptionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> interestIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        interestIds.forEach(id -> jdbcTemplate.update("DELETE FROM interests WHERE id = ?", id));
        userIds.forEach(id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id));
    }

    @Test
    @DisplayName("탈퇴한 사용자를 물리 삭제하면 사용자와 구독이 지워지고 구독한 관심사의 구독자 수가 1씩 준다")
    void givenDeactivatedSubscriber_whenHardDelete_thenSubscriberCountDecreased() {
        User target = saveUser("대상");
        User other = saveUser("다른사람");
        Interest economy = saveInterest("경제");
        Interest sports = saveInterest("스포츠");
        subscribe(target, economy);
        subscribe(other, economy);
        subscribe(target, sports);
        userUseCase.softDeleteUser(target.getId());

        userUseCase.hardDeleteUser(target.getId());

        assertThat(userRepository.findUserIncludingDeleted(target.getId())).isEmpty();
        assertThat(subscriptionRepository.findInterestIdsByUserId(target.getId())).isEmpty();
        assertThat(subscriberCountOf(economy)).isEqualTo(1L);
        assertThat(subscriberCountOf(sports)).isZero();
    }

    @Test
    @DisplayName("구독한 관심사가 없는 탈퇴 사용자도 물리 삭제된다")
    void givenDeactivatedUserWithoutSubscription_whenHardDelete_thenDeleted() {
        User target = saveUser("구독없음");
        userUseCase.softDeleteUser(target.getId());

        userUseCase.hardDeleteUser(target.getId());

        assertThat(userRepository.findUserIncludingDeleted(target.getId())).isEmpty();
    }

    @Test
    @DisplayName("탈퇴하지 않은 사용자를 물리 삭제하면 USER_NOT_FOUND 예외가 나고 구독자 수도 그대로다")
    void givenActiveUser_whenHardDelete_thenNotDeleted() {
        User target = saveUser("활성");
        Interest economy = saveInterest("경제");
        subscribe(target, economy);

        assertThatThrownBy(() -> userUseCase.hardDeleteUser(target.getId()))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
        assertThat(userRepository.findUser(target.getId())).isPresent();
        assertThat(subscriberCountOf(economy)).isEqualTo(1L);
    }

    private User saveUser(String nickname) {
        User saved =
                userRepository.save(
                        User.create(UUID.randomUUID() + "@monew.com", nickname, "password1!"));
        userIds.add(saved.getId());
        return saved;
    }

    private Interest saveInterest(String name) {
        Interest saved =
                interestRepository.save(
                        Interest.create(name + UUID.randomUUID(), List.of(name + "키워드")));
        interestIds.add(saved.getId());
        return saved;
    }

    private void subscribe(User user, Interest interest) {
        subscriptionRepository.save(Subscription.create(user, interest));
        jdbcTemplate.update(
                "UPDATE interests SET subscriber_count = subscriber_count + 1 WHERE id = ?",
                interest.getId());
    }

    private long subscriberCountOf(Interest interest) {
        return interestRepository.findById(interest.getId()).orElseThrow().getSubscriberCount();
    }
}
