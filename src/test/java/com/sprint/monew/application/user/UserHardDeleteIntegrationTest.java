package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.adapter.out.repository.interest.InterestRepositoryAdapter;
import com.sprint.monew.adapter.out.repository.interest.SubscriptionRepositoryAdapter;
import com.sprint.monew.adapter.out.repository.user.UserRepositoryAdapter;
import com.sprint.monew.application.user.provided.service.IUserService;
import com.sprint.monew.application.user.provided.usecase.UserUseCase;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import com.sprint.monew.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class UserHardDeleteIntegrationTest {

    @Autowired private IUserService userService;
    @Autowired private PlatformTransactionManager transactionManager;
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
        assertThat(subscriptionCountOf(target)).isZero();
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
    @DisplayName("같은 관심사를 구독한 탈퇴 사용자들을 함께 삭제하면 실제 삭제한 구독 수만큼 감소한다")
    void givenUsersSharingInterest_whenHardDeleteUsers_thenDecreaseBySubscriptionCounts() {
        User first = saveUser("대상1");
        User second = saveUser("대상2");
        User remaining = saveUser("남은사용자");
        Interest economy = saveInterest("경제");
        Interest sports = saveInterest("스포츠");
        Interest technology = saveInterest("기술");
        subscribe(first, economy);
        subscribe(second, economy);
        subscribe(remaining, economy);
        subscribe(first, sports);
        subscribe(remaining, technology);
        userUseCase.softDeleteUser(first.getId());
        userUseCase.softDeleteUser(second.getId());

        int deleted =
                userService.hardDeleteUsers(
                        List.of(first.getId(), second.getId(), first.getId(), remaining.getId()));

        assertThat(deleted).isEqualTo(2);
        assertThat(userRepository.findUserIncludingDeleted(first.getId())).isEmpty();
        assertThat(userRepository.findUserIncludingDeleted(second.getId())).isEmpty();
        assertThat(userRepository.findUser(remaining.getId())).isPresent();
        assertThat(subscriptionCountOf(first)).isZero();
        assertThat(subscriptionCountOf(second)).isZero();
        assertThat(subscriptionCountOf(remaining)).isEqualTo(2L);
        assertThat(subscriberCountOf(economy)).isEqualTo(1L);
        assertThat(subscriberCountOf(sports)).isZero();
        assertThat(subscriberCountOf(technology)).isEqualTo(1L);
    }

    @Test
    @DisplayName("묶음 삭제 트랜잭션이 실패하면 사용자 삭제와 관심사별 구독자 수 감소가 함께 롤백된다")
    void givenFailedTransaction_whenHardDeleteUsers_thenRollBackCountsAndUsers() {
        User first = saveUser("롤백대상1");
        User second = saveUser("롤백대상2");
        Interest economy = saveInterest("경제");
        subscribe(first, economy);
        subscribe(second, economy);
        userUseCase.softDeleteUser(first.getId());
        userUseCase.softDeleteUser(second.getId());

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(
                        () ->
                                transaction.executeWithoutResult(
                                        status -> {
                                            assertThat(
                                                            userService.hardDeleteUsers(
                                                                    List.of(
                                                                            first.getId(),
                                                                            second.getId())))
                                                    .isEqualTo(2);
                                            assertThat(subscriberCountOf(economy)).isZero();
                                            throw new IllegalStateException("삭제 후 트랜잭션 실패");
                                        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("삭제 후 트랜잭션 실패");

        assertThat(userRepository.findUserIncludingDeleted(first.getId())).isPresent();
        assertThat(userRepository.findUserIncludingDeleted(second.getId())).isPresent();
        assertThat(subscriptionCountOf(first)).isEqualTo(1L);
        assertThat(subscriptionCountOf(second)).isEqualTo(1L);
        assertThat(subscriberCountOf(economy)).isEqualTo(2L);
    }

    @Test
    @DisplayName("구독자 수가 실제 구독보다 작아도 묶음 삭제 후 음수가 되지 않는다")
    void givenUndercountedInterest_whenHardDeleteUsers_thenKeepCountNonNegative() {
        User first = saveUser("대상1");
        User second = saveUser("대상2");
        Interest economy = saveInterest("경제");
        subscribe(first, economy);
        subscribe(second, economy);
        userUseCase.softDeleteUser(first.getId());
        userUseCase.softDeleteUser(second.getId());
        jdbcTemplate.update(
                "UPDATE interests SET subscriber_count = 1 WHERE id = ?", economy.getId());

        assertThat(userService.hardDeleteUsers(List.of(first.getId(), second.getId())))
                .isEqualTo(2);

        assertThat(subscriberCountOf(economy)).isZero();
        assertThat(subscriptionCountOf(first)).isZero();
        assertThat(subscriptionCountOf(second)).isZero();
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

    @Test
    @DisplayName("같은 탈퇴 사용자를 동시에 두 번 물리 삭제해도 한 번만 지워지고 구독자 수도 한 번만 준다")
    void givenConcurrentHardDelete_whenHardDeleteTwice_thenDeletedOnce() throws Exception {
        User target = saveUser("동시삭제");
        User other = saveUser("다른사람");
        Interest economy = saveInterest("경제");
        subscribe(target, economy);
        subscribe(other, economy);
        userUseCase.softDeleteUser(target.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> hardDelete =
                () -> {
                    start.await();
                    try {
                        userUseCase.hardDeleteUser(target.getId());
                        return true;
                    } catch (MonewException e) {
                        return false;
                    }
                };
        Future<Boolean> first = executor.submit(hardDelete);
        Future<Boolean> second = executor.submit(hardDelete);
        start.countDown();
        List<Boolean> results =
                List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(userRepository.findUserIncludingDeleted(target.getId())).isEmpty();
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

    private long subscriptionCountOf(User user) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subscriptions WHERE user_id = ?", Long.class, user.getId());
    }
}
