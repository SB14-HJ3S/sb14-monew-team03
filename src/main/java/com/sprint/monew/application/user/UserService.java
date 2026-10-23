package com.sprint.monew.application.user;

import com.sprint.monew.application.user.required.UserRepositoryPort;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepositoryPort repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getActiveUser(UUID userId) {
        return repository
                .findActiveById(userId)
                .orElseThrow(
                        () ->
                                new MonewException(
                                        UserErrorType.USER_NOT_FOUND, Map.of("userId", userId)));
    }

    @Override
    public List<User> getActiveUsers(List<UUID> userIds) {
        return List.of();
    }

    @Override
    @Transactional
    public User register(String email, String nickname, String password) {
        if (repository.existsByEmail(email)) {
            throw new MonewException(UserErrorType.USER_EMAIL_DUPLICATED);
        }
        String encodedPassword = passwordEncoder.encode(password);
        return repository.save(User.create(email, nickname, encodedPassword));
    }
}
