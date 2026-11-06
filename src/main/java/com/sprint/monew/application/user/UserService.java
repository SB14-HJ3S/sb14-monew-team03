package com.sprint.monew.application.user;

import com.sprint.monew.application.user.event.UserHardDeletingEvent;
import com.sprint.monew.application.user.event.UserNicknameUpdatedEvent;
import com.sprint.monew.application.user.event.UserRegisteredEvent;
import com.sprint.monew.application.user.required.UserEventPublisherPort;
import com.sprint.monew.application.user.required.UserRepositoryPort;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements IUserService {

    private final UserRepositoryPort repository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisherPort userEventPublisher;
    private final String dummyPasswordHash;

    public UserService(
            UserRepositoryPort repository,
            PasswordEncoder passwordEncoder,
            UserEventPublisherPort userEventPublisher) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.userEventPublisher = userEventPublisher;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

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
        Optional<User> user = repository.findUserByEmail(email);
        String passwordHash = user.map(User::getPassword).orElse(dummyPasswordHash);
        boolean matched = passwordEncoder.matches(password, passwordHash);
        if (user.isEmpty() || !matched) {
            throw loginFailed();
        }
        return user.get();
    }

    @Override
    @Transactional
    public User updateNickname(UUID userId, String nickname) {
        User user = getActiveUser(userId);
        user.changeNickname(nickname);
        userEventPublisher.publish(new UserNicknameUpdatedEvent(userId, user.getNickname()));
        return user;
    }

    @Override
    @Transactional
    public void softDeleteUser(UUID userId) {
        User user = getActiveUser(userId);
        user.softDelete();
    }

    @Override
    @Transactional
    public void hardDeleteUser(UUID userId) {
        User user =
                repository
                        .findUserIncludingDeletedForUpdate(userId)
                        .filter(User::isDeleted)
                        .orElseThrow(() -> userNotFound(userId));
        removeCompletely(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findCleanupTargetIds(Instant threshold) {
        return repository.findDeletedUserIdsBefore(threshold);
    }

    @Override
    @Transactional
    public int hardDeleteUsers(List<UUID> userIds) {
        List<UUID> targetIds =
                repository.findDeletedUsersForUpdate(userIds).stream().map(User::getId).toList();
        if (targetIds.isEmpty()) {
            return 0;
        }
        userEventPublisher.publish(new UserHardDeletingEvent(targetIds));
        return repository.deleteUsers(targetIds);
    }

    private void removeCompletely(User user) {
        userEventPublisher.publish(new UserHardDeletingEvent(List.of(user.getId())));
        repository.deleteById(user.getId());
    }

    private MonewException userNotFound(UUID userId) {
        return new MonewException(UserErrorType.USER_NOT_FOUND, Map.of("userId", userId));
    }

    private MonewException loginFailed() {
        return new MonewException(UserErrorType.USER_LOGIN_FAILED);
    }
}
