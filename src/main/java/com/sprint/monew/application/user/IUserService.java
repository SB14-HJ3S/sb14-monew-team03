package com.sprint.monew.application.user;

import com.sprint.monew.domain.user.entity.User;
import java.util.List;
import java.util.UUID;

public interface IUserService {
    User getActiveUser(UUID userId);

    List<User> getActiveUsers(List<UUID> userIds);

    User register(String email, String nickname, String password);

    User login(String email, String password);

    User updateNickname(UUID userId, String nickname);

    void softDeleteUser(UUID userId);
}
