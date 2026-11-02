package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Test
    @DisplayName("탈퇴한 지 1일 지난 사용자를 한 명씩 물리 삭제하고 지운 수를 돌려준다")
    void givenCleanupTargets_whenCleanupDeletedUsers_thenHardDeleteEachAndReturnCount() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        given(userService.findCleanupTargetIds(any())).willReturn(List.of(first, second));
        Instant lowerBound = Instant.now().minus(Duration.ofDays(1));

        int cleaned = userApplication.cleanupDeletedUsers();

        Instant upperBound = Instant.now().minus(Duration.ofDays(1));
        verify(userService).findCleanupTargetIds(thresholdCaptor.capture());
        assertThat(thresholdCaptor.getValue()).isBetween(lowerBound, upperBound);
        verify(userService).hardDeleteUser(first);
        verify(userService).hardDeleteUser(second);
        assertThat(cleaned).isEqualTo(2);
    }

    @Test
    @DisplayName("한 명을 지우다 실패해도 나머지는 지우고, 지운 수만 돌려준다")
    void givenOneUserFails_whenCleanupDeletedUsers_thenOthersStillDeleted() {
        UUID first = UUID.randomUUID();
        UUID failing = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        given(userService.findCleanupTargetIds(any())).willReturn(List.of(first, failing, third));
        lenient()
                .doThrow(new IllegalStateException("DB 오류"))
                .when(userService)
                .hardDeleteUser(failing);

        int cleaned = userApplication.cleanupDeletedUsers();

        verify(userService).hardDeleteUser(first);
        verify(userService).hardDeleteUser(failing);
        verify(userService).hardDeleteUser(third);
        assertThat(cleaned).isEqualTo(2);
    }

    @Test
    @DisplayName("그사이 이미 지워진 사용자는 건너뛰고 지운 수에 넣지 않는다")
    void givenAlreadyRemovedUser_whenCleanupDeletedUsers_thenSkip() {
        UUID removed = UUID.randomUUID();
        given(userService.findCleanupTargetIds(any())).willReturn(List.of(removed));
        willThrow(new MonewException(UserErrorType.USER_NOT_FOUND))
                .given(userService)
                .hardDeleteUser(removed);

        int cleaned = userApplication.cleanupDeletedUsers();

        assertThat(cleaned).isZero();
    }

    @Test
    @DisplayName("지울 사용자가 없으면 0을 돌려준다")
    void givenNoTargets_whenCleanupDeletedUsers_thenZero() {
        given(userService.findCleanupTargetIds(any())).willReturn(List.of());

        int cleaned = userApplication.cleanupDeletedUsers();

        assertThat(cleaned).isZero();
        verify(userService, never()).hardDeleteUser(any());
    }
}
