package com.sprint.monew.application.user;

import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.annotation.ApplicationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

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
}
