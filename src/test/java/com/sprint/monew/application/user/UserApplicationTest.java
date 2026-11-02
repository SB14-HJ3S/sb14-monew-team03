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
    void givenPurgeTargets_whenPurgeDeletedUsers_thenHardDeleteEachAndReturnCount() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        given(userService.findPurgeTargetIds(any())).willReturn(List.of(first, second));
        Instant lowerBound = Instant.now().minus(Duration.ofDays(1));

        int purged = userApplication.purgeDeletedUsers();

        Instant upperBound = Instant.now().minus(Duration.ofDays(1));
        verify(userService).findPurgeTargetIds(thresholdCaptor.capture());
        assertThat(thresholdCaptor.getValue()).isBetween(lowerBound, upperBound);
        verify(userService).hardDeleteUser(first);
        verify(userService).hardDeleteUser(second);
        assertThat(purged).isEqualTo(2);
    }

    @Test
    @DisplayName("한 명을 지우다 실패해도 나머지는 지우고, 지운 수만 돌려준다")
    void givenOneUserFails_whenPurgeDeletedUsers_thenOthersStillDeleted() {
        UUID first = UUID.randomUUID();
        UUID failing = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        given(userService.findPurgeTargetIds(any())).willReturn(List.of(first, failing, third));
        lenient()
                .doThrow(new IllegalStateException("DB 오류"))
                .when(userService)
                .hardDeleteUser(failing);

        int purged = userApplication.purgeDeletedUsers();

        verify(userService).hardDeleteUser(first);
        verify(userService).hardDeleteUser(failing);
        verify(userService).hardDeleteUser(third);
        assertThat(purged).isEqualTo(2);
    }

    @Test
    @DisplayName("그사이 이미 지워진 사용자는 건너뛰고 지운 수에 넣지 않는다")
    void givenAlreadyRemovedUser_whenPurgeDeletedUsers_thenSkip() {
        UUID removed = UUID.randomUUID();
        given(userService.findPurgeTargetIds(any())).willReturn(List.of(removed));
        willThrow(new MonewException(UserErrorType.USER_NOT_FOUND))
                .given(userService)
                .hardDeleteUser(removed);

        int purged = userApplication.purgeDeletedUsers();

        assertThat(purged).isZero();
    }

    @Test
    @DisplayName("지울 사용자가 없으면 0을 돌려준다")
    void givenNoTargets_whenPurgeDeletedUsers_thenZero() {
        given(userService.findPurgeTargetIds(any())).willReturn(List.of());

        int purged = userApplication.purgeDeletedUsers();

        assertThat(purged).isZero();
        verify(userService, never()).hardDeleteUser(any());
    }
}
