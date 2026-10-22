package com.sprint.monew.application.user.provided;

import com.sprint.monew.domain.user.entity.User;
import java.util.UUID;

public interface UserUseCase {

    User getActiveUser(UUID userId);
}
