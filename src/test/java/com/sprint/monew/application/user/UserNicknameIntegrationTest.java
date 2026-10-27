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
class UserNicknameIntegrationTest {

    @Autowired private UserUseCase userUseCase;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    @DisplayName("닉네임을 바꾸면 DB에도 바뀐 닉네임이 저장된다")
    void givenActiveUser_whenUpdateNickname_thenSavedToDatabase() {
        User saved =
                userRepository.save(
                        User.create(UUID.randomUUID() + "@monew.com", "옛닉네임", "password1!"));
        userId = saved.getId();

        userUseCase.updateNickname(userId, "새닉네임");

        assertThat(userRepository.findUser(userId))
                .get()
                .extracting(User::getNickname)
                .isEqualTo("새닉네임");
    }
}
