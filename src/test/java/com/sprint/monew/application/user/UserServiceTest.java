package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.monew.application.user.event.UserHardDeletingEvent;
import com.sprint.monew.application.user.event.UserNicknameUpdatedEvent;
import com.sprint.monew.application.user.event.UserRegisteredEvent;
import com.sprint.monew.application.user.required.UserEventPublisherPort;
import com.sprint.monew.application.user.required.UserRepositoryPort;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepositoryPort repository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private UserEventPublisherPort userEventPublisher;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("활성 사용자가 있으면 그 사용자를 돌려준다")
    void givenActiveUser_whenGetActiveUser_thenReturnUser() {
        UUID userId = UUID.randomUUID();
        User user = User.create("test@monew.com", "테스터", "password1!");
        given(repository.findUser(userId)).willReturn(Optional.of(user));

        User result = userService.getActiveUser(userId);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자면 USER_NOT_FOUND 예외가 난다")
    void givenNoActiveUser_whenGetActiveUser_thenThrowUserNotFound() {
        UUID userId = UUID.randomUUID();
        given(repository.findUser(userId)).willReturn(Optional.empty());

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
    @DisplayName("가입이 끝나면 저장된 사용자 정보로 가입 이벤트를 보낸다")
    void givenNewEmail_whenRegister_thenPublishRegisteredEvent() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-26T00:00:00Z");
        User saved = User.create("new@monew.com", "새사람", "암호화된비밀번호");
        ReflectionTestUtils.setField(saved, "id", userId);
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
        given(repository.existsByEmail("new@monew.com")).willReturn(false);
        given(passwordEncoder.encode("password1!")).willReturn("암호화된비밀번호");
        given(repository.save(any(User.class))).willReturn(saved);

        userService.register("new@monew.com", "새사람", "password1!");

        ArgumentCaptor<UserRegisteredEvent> captor =
                ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(userEventPublisher).publish(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(new UserRegisteredEvent(userId, "new@monew.com", "새사람", createdAt));
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
        verifyNoInteractions(userEventPublisher);
    }

    @Test
    @DisplayName("여러 명을 한 번에 조회하면 활성 사용자 목록을 돌려준다")
    void givenUserIds_whenGetActiveUsers_thenReturnActiveUsers() {
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<User> users =
                List.of(
                        User.create("a@monew.com", "에이", "암호화된비밀번호"),
                        User.create("b@monew.com", "비", "암호화된비밀번호"));
        given(repository.findUsers(userIds)).willReturn(users);

        List<User> result = userService.getActiveUsers(userIds);

        assertThat(result).isEqualTo(users);
    }

    @Test
    @DisplayName("빈 목록으로 조회하면 DB를 조회하지 않고 빈 목록을 돌려준다")
    void givenEmptyIds_whenGetActiveUsers_thenReturnEmptyWithoutQuery() {
        List<User> result = userService.getActiveUsers(List.of());

        assertThat(result).isEmpty();
        verify(repository, never()).findUsers(any());
    }

    @Test
    @DisplayName("이메일과 비밀번호가 맞으면 그 사용자를 돌려준다")
    void givenCorrectPassword_whenLogin_thenReturnUser() {
        User user = User.create("login@monew.com", "로그인", "암호화된비밀번호");
        given(repository.findUserByEmail("login@monew.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1!", "암호화된비밀번호")).willReturn(true);

        User result = userService.login("login@monew.com", "password1!");

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 USER_LOGIN_FAILED 예외가 난다")
    void givenWrongPassword_whenLogin_thenThrowLoginFailed() {
        User user = User.create("login@monew.com", "로그인", "암호화된비밀번호");
        given(repository.findUserByEmail("login@monew.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong1!", "암호화된비밀번호")).willReturn(false);

        assertThatThrownBy(() -> userService.login("login@monew.com", "wrong1!"))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_LOGIN_FAILED);
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자의 이메일이면 USER_LOGIN_FAILED 예외가 난다")
    void givenNoActiveUser_whenLogin_thenThrowLoginFailed() {
        given(repository.findUserByEmail("none@monew.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("none@monew.com", "password1!"))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_LOGIN_FAILED);
    }

    @Test
    @DisplayName("닉네임을 바꾸면 닉네임이 바뀐다")
    void givenActiveUser_whenUpdateNickname_thenChanged() {
        UUID userId = UUID.randomUUID();
        User user = User.create("me@monew.com", "옛닉네임", "암호화된비밀번호");
        given(repository.findUser(userId)).willReturn(Optional.of(user));

        User result = userService.updateNickname(userId, "새닉네임");

        assertThat(result.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자면 닉네임 수정 때 USER_NOT_FOUND 예외가 난다")
    void givenNoActiveUser_whenUpdateNickname_thenThrowUserNotFound() {
        UUID userId = UUID.randomUUID();
        given(repository.findUser(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateNickname(userId, "새닉네임"))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("닉네임을 바꾸면 바뀐 닉네임으로 닉네임 수정 이벤트를 보낸다")
    void givenActiveUser_whenUpdateNickname_thenPublishNicknameUpdatedEvent() {
        UUID userId = UUID.randomUUID();
        User user = User.create("me@monew.com", "옛닉네임", "암호화된비밀번호");
        given(repository.findUser(userId)).willReturn(Optional.of(user));

        userService.updateNickname(userId, "새닉네임");

        verify(userEventPublisher).publish(new UserNicknameUpdatedEvent(userId, "새닉네임"));
    }

    @Test
    @DisplayName("탈퇴하면 deletedAt이 채워진다(논리 삭제)")
    void givenActiveUser_whenSoftDelete_thenDeleted() {
        UUID userId = UUID.randomUUID();
        User user = User.create("me@monew.com", "탈퇴할사람", "암호화된비밀번호");
        given(repository.findUser(userId)).willReturn(Optional.of(user));

        userService.softDeleteUser(userId);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        verifyNoInteractions(userEventPublisher);
    }

    @Test
    @DisplayName("이미 탈퇴했거나 없는 사용자면 USER_NOT_FOUND 예외가 난다")
    void givenNoActiveUser_whenSoftDelete_thenThrowUserNotFound() {
        UUID userId = UUID.randomUUID();
        given(repository.findUser(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.softDeleteUser(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("탈퇴한 사용자를 완전 삭제하면 DB에서 지운다")
    void givenDeletedUser_whenHardDelete_thenDeleteById() {
        UUID userId = UUID.randomUUID();
        User user = User.create("me@monew.com", "완전삭제", "암호화된비밀번호");
        ReflectionTestUtils.setField(user, "id", userId);
        user.softDelete();
        given(repository.findUserIncludingDeleted(userId)).willReturn(Optional.of(user));

        userService.hardDeleteUser(userId);

        InOrder inOrder = inOrder(userEventPublisher, repository);
        inOrder.verify(userEventPublisher).publish(new UserHardDeletingEvent(userId));
        inOrder.verify(repository).deleteById(userId);
    }

    @Test
    @DisplayName("탈퇴하지 않은 사용자를 완전 삭제하면 USER_NOT_FOUND 예외가 나고 지우지 않는다")
    void givenActiveUser_whenHardDelete_thenThrowUserNotFound() {
        UUID userId = UUID.randomUUID();
        User user = User.create("me@monew.com", "활성사용자", "암호화된비밀번호");
        given(repository.findUserIncludingDeleted(userId)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.hardDeleteUser(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("없는 사용자를 완전 삭제하면 USER_NOT_FOUND 예외가 난다")
    void givenNoUser_whenHardDelete_thenThrowUserNotFound() {
        UUID userId = UUID.randomUUID();
        given(repository.findUserIncludingDeleted(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.hardDeleteUser(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("탈퇴한 사용자면 물리 삭제 대상 확인을 통과한다")
    void givenDeletedUser_whenValidateDeactivatedUser_thenPass() {
        UUID userId = UUID.randomUUID();
        User user = User.create("me@monew.com", "탈퇴자", "암호화된비밀번호");
        user.softDelete();
        given(repository.findUserIncludingDeleted(userId)).willReturn(Optional.of(user));

        assertThatCode(() -> userService.validateDeactivatedUser(userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("탈퇴하지 않은 사용자면 물리 삭제 대상 확인에서 USER_NOT_FOUND 예외가 난다")
    void givenActiveUser_whenValidateDeactivatedUser_thenThrowUserNotFound() {
        UUID userId = UUID.randomUUID();
        User user = User.create("me@monew.com", "활성사용자", "암호화된비밀번호");
        given(repository.findUserIncludingDeleted(userId)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.validateDeactivatedUser(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 사용자면 물리 삭제 대상 확인에서 USER_NOT_FOUND 예외가 난다")
    void givenNoUser_whenValidateDeactivatedUser_thenThrowUserNotFound() {
        UUID userId = UUID.randomUUID();
        given(repository.findUserIncludingDeleted(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.validateDeactivatedUser(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
    }
}
