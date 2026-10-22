package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.sprint.monew.application.user.required.UserRepositoryPort;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepositoryPort repository;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("활성 사용자가 있으면 그 사용자를 돌려준다")
    void givenActiveUser_whenGetActiveUser_thenReturnUser() {

        UUID userId = UUID.randomUUID();
        User user = User.create("test@monew.com", "테스터", "password1!");
        given(repository.findActiveById(userId)).willReturn(Optional.of(user));

        User result = userService.getActiveUser(userId);
        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자면 USER_NOT_FOUND 예외가 난다")
    void givenNoActiveUser_whenGetActiveUser_thenThrowUserNotFound() {

        UUID userId = UUID.randomUUID();
        given(repository.findActiveById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getActiveUser(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
    }
}
