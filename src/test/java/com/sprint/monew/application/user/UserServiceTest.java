package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.user.required.UserRepositoryPort;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepositoryPort repository;

    @Mock private PasswordEncoder passwordEncoder;

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

    @Test
    @DisplayName("처음 쓰는 이메일이면 비밀번호를 암호화해서 저장한다")
    void givenNewEmail_whenRegister_thenSaveWithEncodedPassword() {
        given(repository.existsByEmail("new@monew.com")).willReturn(false);
        given(passwordEncoder.encode("password1!")).willReturn("암호화된비밀번호");
        given(repository.save(any(User.class))).willAnswer(AdditionalAnswers.returnsFirstArg());

        User result = userService.register("new@monew.com", "새사람", "password1!");

        assertThat(result.getEmail()).isEqualTo("new@monew.com");
        assertThat(result.getNickname()).isEqualTo("새사람");
        assertThat(result.getPassword()).isEqualTo("암호화된비밀번호");
    }

    @Test
    @DisplayName("이미 있는 이메일이면 USER_EMAIL_DUPLICATED 예외가 나고 저장하지 않는다")
    void givenDuplicatedEmail_whenRegister_thenThrowEmailDuplicated() {
        given(repository.existsByEmail("dup@monew.com")).willReturn(true);

        assertThatThrownBy(() -> userService.register("dup@monew.com", "중복", "password1!"))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_EMAIL_DUPLICATED);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("여러 명을 한 번에 조회하면 활성 사용자 목록을 돌려준다")
    void givenUserIds_whenGetActiveUsers_thenReturnActiveUsers() {
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<User> users =
                List.of(
                        User.create("a@monew.com", "에이", "암호화된비밀번호"),
                        User.create("b@monew.com", "비", "암호화된비밀번호"));
        given(repository.findAllActiveByIds(userIds)).willReturn(users);

        List<User> result = userService.getActiveUsers(userIds);

        assertThat(result).isEqualTo(users);
    }

    @Test
    @DisplayName("빈 목록으로 조회하면 DB를 조회하지 않고 빈 목록을 돌려준다")
    void givenEmptyIds_whenGetActiveUsers_thenReturnEmptyWithoutQuery() {
        List<User> result = userService.getActiveUsers(List.of());

        assertThat(result).isEmpty();
        verify(repository, never()).findAllActiveByIds(any());
    }
}
