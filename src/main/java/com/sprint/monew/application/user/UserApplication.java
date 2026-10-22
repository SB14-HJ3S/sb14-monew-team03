package com.sprint.monew.application.user;

import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.domain.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserApplication implements UserUseCase {

    private final IUserService userService;

    @Override
    public User getActiveUser(UUID userId) {
        return userService.getActiveUser(userId);
    }
}
