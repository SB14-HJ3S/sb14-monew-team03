package com.sprint.monew.application.user.provided.usecase;

import com.sprint.monew.application.user.provided.usecase.dto.UserResult;
import com.sprint.monew.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public interface UserUseCase {

    User getActiveUser(@NotNull UUID userId);

    UserResult register(
            @NotBlank String email, @NotBlank String nickname, @NotBlank String password);

    UserResult login(@NotBlank String email, @NotBlank String password);

    UserResult updateNickname(@NotNull UUID userId, @NotBlank String nickname);

    void softDeleteUser(@NotNull UUID userId);

    void hardDeleteUser(@NotNull UUID userId);

    int cleanupDeletedUsers();
}
