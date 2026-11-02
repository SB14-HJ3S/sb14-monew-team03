package com.sprint.monew.application.user;

import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.annotation.ApplicationService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationService
@RequiredArgsConstructor
public class UserApplication implements UserUseCase {

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
    public int purgeDeletedUsers() {
        Instant threshold = Instant.now().minus(Duration.ofDays(1));
        int purged = 0;
        for (UUID userId : userService.findPurgeTargetIds(threshold)) {
            try {
                userService.hardDeleteUser(userId);
                purged++;
            } catch (RuntimeException e) {
                log.error("사용자 완전 삭제 실패: userId={}", userId, e);
            }
        }
        return purged;
    }
}
