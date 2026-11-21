package com.sprint.monew.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.user.provided.service.IUserService;
import com.sprint.monew.application.user.provided.usecase.dto.UserResult;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
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
    @DisplayName("탈퇴한 지 1일 지난 사용자를 한 번에 물리 삭제하고 지운 수를 돌려준다")
    void givenCleanupTargets_whenCleanupDeletedUsers_thenHardDeleteAtOnceAndReturnCount() {
        List<UUID> targets = List.of(UUID.randomUUID(), UUID.randomUUID());
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        given(userService.findCleanupTargetIds(any())).willReturn(targets);
        given(userService.hardDeleteUsers(targets)).willReturn(2);
        Instant lowerBound = Instant.now().minus(Duration.ofDays(1));

        int deleted = userApplication.cleanupDeletedUsers();

        Instant upperBound = Instant.now().minus(Duration.ofDays(1));
        verify(userService).findCleanupTargetIds(thresholdCaptor.capture());
        assertThat(thresholdCaptor.getValue()).isBetween(lowerBound, upperBound);
        assertThat(deleted).isEqualTo(2);
    }

    @Test
    @DisplayName("대상이 100명을 넘으면 100명씩 나눠서 지운다")
    void givenMoreThanChunkSize_whenCleanupDeletedUsers_thenDeleteInChunks() {
        List<UUID> targets = Stream.generate(UUID::randomUUID).limit(101).toList();
        given(userService.findCleanupTargetIds(any())).willReturn(targets);
        given(userService.hardDeleteUsers(targets.subList(0, 100))).willReturn(100);
        given(userService.hardDeleteUsers(targets.subList(100, 101))).willReturn(1);

        int deleted = userApplication.cleanupDeletedUsers();

        assertThat(deleted).isEqualTo(101);
    }

    @Test
    @DisplayName("묶음 삭제가 실패하면 그 묶음을 한 명씩 다시 지우고, 실패한 사람만 빼고 센다")
    void givenChunkFails_whenCleanupDeletedUsers_thenRetryOneByOne() {
        UUID first = UUID.randomUUID();
        UUID failing = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        List<UUID> targets = List.of(first, failing, third);
        given(userService.findCleanupTargetIds(any())).willReturn(targets);
        willThrow(new IllegalStateException("DB 오류")).given(userService).hardDeleteUsers(targets);
        lenient()
                .doThrow(new IllegalStateException("DB 오류"))
                .when(userService)
                .hardDeleteUser(failing);

        int deleted = userApplication.cleanupDeletedUsers();

        verify(userService).hardDeleteUser(first);
        verify(userService).hardDeleteUser(failing);
        verify(userService).hardDeleteUser(third);
        assertThat(deleted).isEqualTo(2);
    }

    @Test
    @DisplayName("지울 사용자가 없으면 0을 돌려준다")
    void givenNoTargets_whenCleanupDeletedUsers_thenZero() {
        given(userService.findCleanupTargetIds(any())).willReturn(List.of());

        int deleted = userApplication.cleanupDeletedUsers();

        assertThat(deleted).isZero();
        verify(userService, never()).hardDeleteUsers(any());
    }

    @Test
    @DisplayName("한 명씩 다시 지울 때 이미 지워진 사용자(USER_NOT_FOUND)는 건너뛰고 지운 수에 넣지 않는다")
    void givenAlreadyDeletedUser_whenRetryOneByOne_thenSkipAndNotCount() {
        UUID first = UUID.randomUUID();
        UUID gone = UUID.randomUUID();
        List<UUID> targets = List.of(first, gone);
        given(userService.findCleanupTargetIds(any())).willReturn(targets);
        willThrow(new IllegalStateException("DB 오류")).given(userService).hardDeleteUsers(targets);
        lenient()
                .doThrow(new MonewException(UserErrorType.USER_NOT_FOUND))
                .when(userService)
                .hardDeleteUser(gone);

        int deleted = userApplication.cleanupDeletedUsers();

        verify(userService).hardDeleteUser(first);
        verify(userService).hardDeleteUser(gone);
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    @DisplayName("한 명씩 다시 지울 때 USER_NOT_FOUND가 아닌 MonewException은 실패로 세고 다음 사용자를 계속 지운다")
    void givenOtherMonewException_whenRetryOneByOne_thenCountAsFailureAndContinue() {
        UUID failing = UUID.randomUUID();
        UUID next = UUID.randomUUID();
        List<UUID> targets = List.of(failing, next);
        given(userService.findCleanupTargetIds(any())).willReturn(targets);
        willThrow(new IllegalStateException("DB 오류")).given(userService).hardDeleteUsers(targets);
        willThrow(new MonewException(UserErrorType.USER_EMAIL_DUPLICATED))
                .given(userService)
                .hardDeleteUser(failing);

        int deleted = userApplication.cleanupDeletedUsers();

        verify(userService).hardDeleteUser(failing);
        verify(userService).hardDeleteUser(next);
        assertThat(deleted).isEqualTo(1);
    }
}
