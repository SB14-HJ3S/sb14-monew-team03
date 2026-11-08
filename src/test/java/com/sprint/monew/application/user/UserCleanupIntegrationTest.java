package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.repository.user.UserRepositoryAdapter;
import com.sprint.monew.application.user.provided.usecase.UserUseCase;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UserCleanupIntegrationTest {

    @Autowired private UserUseCase userUseCase;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<UUID> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        userIds.forEach(id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id));
    }

    @Test
    @DisplayName("탈퇴한 지 1일 지난 사용자만 완전 삭제되고, 1일 안 된 탈퇴자와 정상 사용자는 남는다")
    void givenMixedUsers_whenCleanupDeletedUsers_thenOnlyOldDeletedUserRemoved() {
        User oldDeleted = saveUserDeletedAgo("old", Duration.ofDays(2));
        User recentDeleted = saveUserDeletedAgo("recent", Duration.ofHours(1));
        User active = saveUser("active");

        int deleted = userUseCase.cleanupDeletedUsers();

        assertThat(deleted).isEqualTo(1);
        assertThat(userRepository.findUserIncludingDeleted(oldDeleted.getId())).isEmpty();
        assertThat(userRepository.findUserIncludingDeleted(recentDeleted.getId())).isPresent();
        assertThat(userRepository.findUserIncludingDeleted(active.getId())).isPresent();
    }

    private User saveUser(String name) {
        User user = userRepository.save(User.create(name + "@monew.com", name, "password1!"));
        userIds.add(user.getId());
        return user;
    }

    private User saveUserDeletedAgo(String name, Duration ago) {
        User user = saveUser(name);
        jdbcTemplate.update(
                "UPDATE users SET deleted_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(ago)),
                user.getId());
        return user;
    }
}
