package com.sprint.monew.application.user.provided;

import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.domain.user.entity.User;
import java.util.UUID;

public interface UserUseCase {

    User getActiveUser(UUID userId);

    UserResult register(String email, String nickname, String password);

    UserResult login(String email, String password);

    UserResult updateNickname(UUID userId, String nickname);

    void softDeleteUser(UUID userId);

    void hardDeleteUser(UUID userId);
}
