package com.sprint.monew.application.user;

import com.sprint.monew.application.user.required.UserRepositoryPort;
import com.sprint.monew.domain.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepositoryPort repository;

    @Override
    public User getActiveUser(UUID userId) {
        return null;
    }
}
