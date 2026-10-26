package com.sprint.monew.application.user;

import com.sprint.monew.application.user.event.UserRegisteredEvent;
import com.sprint.monew.application.user.required.UserEventPublisherPort;
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
    private final UserEventPublisherPort userEventPublisher;

    @Override
    public User getActiveUser(UUID userId) {
        return repository
                .findUser(userId)
                .orElseThrow(
                        () ->
                                new MonewException(
                                        UserErrorType.USER_NOT_FOUND, Map.of("userId", userId)));
    }

    @Override
    public List<User> getActiveUsers(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return repository.findUsers(userIds);
    }

    @Override
    @Transactional
    public User register(String email, String nickname, String password) {
        if (repository.existsByEmail(email)) {
            throw new MonewException(UserErrorType.USER_EMAIL_DUPLICATED);
        }
        String encodedPassword = passwordEncoder.encode(password);
        User user = repository.save(User.create(email, nickname, encodedPassword));
        userEventPublisher.publish(UserRegisteredEvent.from(user));
        return user;
    }

    @Override
    public User login(String email, String password) {
        User user = repository.findUserByEmail(email).orElseThrow(this::loginFailed);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw loginFailed();
        }
        return user;
    }

    @Override
    @Transactional
    public User updateNickname(UUID userId, String nickname) {
        User user =
                repository
                        .findUser(userId)
                        .orElseThrow(
                                () ->
                                        new MonewException(
                                                UserErrorType.USER_NOT_FOUND,
                                                Map.of("userId", userId)));
        user.changeNickname(nickname);
        return user;
    }

    private MonewException loginFailed() {
        return new MonewException(UserErrorType.USER_LOGIN_FAILED);
    }
}
