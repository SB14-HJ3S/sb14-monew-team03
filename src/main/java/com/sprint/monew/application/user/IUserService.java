package com.sprint.monew.application.user;

import com.sprint.monew.domain.user.entity.User;
import java.util.UUID;

public interface IUserService {
    User getActiveUser(UUID userId);

    User register(String email, String nickname, String password);
}
