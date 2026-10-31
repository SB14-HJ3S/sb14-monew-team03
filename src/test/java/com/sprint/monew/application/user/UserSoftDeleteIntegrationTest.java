package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UserSoftDeleteIntegrationTest {

    @Autowired private UserUseCase userUseCase;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    @DisplayName("탈퇴하면 DB에 탈퇴 시각이 저장되고 활성 사용자 조회에서 빠진다")
    void givenActiveUser_whenSoftDelete_thenSavedToDatabase() {
        User saved =
                userRepository.save(
                        User.create(UUID.randomUUID() + "@monew.com", "탈퇴할사람", "password1!"));
        userId = saved.getId();

        userUseCase.softDeleteUser(userId);

        assertThat(userRepository.findById(userId))
                .get()
                .extracting(User::getDeletedAt)
                .isNotNull();
        assertThat(userRepository.findUser(userId)).isEmpty();
    }
}
