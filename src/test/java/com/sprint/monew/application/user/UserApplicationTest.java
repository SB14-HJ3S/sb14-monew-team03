package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.domain.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserApplicationTest {

    @Mock private IUserService userService;

    @InjectMocks private UserApplication userApplication;

    @Test
    @DisplayName("회원가입하면 저장된 사용자를 UserResult로 바꿔 돌려준다")
    void givenRegisteredUser_whenRegister_thenReturnUserResult() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-21T00:00:00Z");
        User user = User.create("new@monew.com", "새사람", "암호화된비밀번호");
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "createdAt", createdAt);
        given(userService.register("new@monew.com", "새사람", "password1!")).willReturn(user);

        UserResult result = userApplication.register("new@monew.com", "새사람", "password1!");

        assertThat(result).isEqualTo(new UserResult(userId, "new@monew.com", "새사람", createdAt));
    }
}
