package com.sprint.monew.application.user;

import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.annotation.ApplicationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationService
@RequiredArgsConstructor
public class UserApplication implements UserUseCase {

    private static final int DELETED_USER_RETENTION_DAYS = 1;
    private static final int CLEANUP_CHUNK_SIZE = 100;

    private final IUserService userService;

    @Override
    public User getActiveUser(UUID userId) {
        return userService.getActiveUser(userId);
    }

    @Override
    public UserResult register(String email, String nickname, String password) {
        User user = userService.register(email, nickname, password);
        return UserResult.from(user);
    }

    @Override
    public UserResult login(String email, String password) {
        User user = userService.login(email, password);
        return UserResult.from(user);
    }

    @Override
    public UserResult updateNickname(UUID userId, String nickname) {
        User user = userService.updateNickname(userId, nickname);
        return UserResult.from(user);
    }

    @Override
    public void softDeleteUser(UUID userId) {
        userService.softDeleteUser(userId);
    }

    @Override
    public void hardDeleteUser(UUID userId) {
        userService.hardDeleteUser(userId);
    }

    @Override
    public int cleanupDeletedUsers() {
        Instant threshold = Instant.now().minus(DELETED_USER_RETENTION_DAYS, ChronoUnit.DAYS);
        List<UUID> targetIds = userService.findCleanupTargetIds(threshold);
        int deleted = 0;
        for (int from = 0; from < targetIds.size(); from += CLEANUP_CHUNK_SIZE) {
            int to = Math.min(from + CLEANUP_CHUNK_SIZE, targetIds.size());
            deleted += cleanup(List.copyOf(targetIds.subList(from, to)));
        }
        log.info(
                "탈퇴 후 {}일 지난 사용자 완전 삭제: {}명 (기준 시각={})",
                DELETED_USER_RETENTION_DAYS,
                deleted,
                threshold);
        return deleted;
    }

    private int cleanup(List<UUID> userIds) {
        try {
            return userService.hardDeleteUsers(userIds);
        } catch (RuntimeException e) {
            log.error("사용자 완전 삭제 실패, 다음 실행 때 다시 시도: {}명, userIds={}", userIds.size(), userIds, e);
            return 0;
        }
    }
}
