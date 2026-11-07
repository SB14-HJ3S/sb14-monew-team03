package com.sprint.monew.adapter.out.repository.user;

import com.sprint.monew.application.user.required.UserRepositoryPort;
import com.sprint.monew.domain.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<User> findUser(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return jpaRepository.findByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findUsers(List<UUID> ids) {
        return jpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public Optional<User> findUserIncludingDeleted(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<User> findUserIncludingDeletedForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id);
    }

    @Override
    public List<UUID> findDeletedUserIdsBefore(Instant threshold) {
        return jpaRepository.findIdsByDeletedAtBefore(threshold);
    }

    @Override
    public List<User> findDeletedUsersForUpdate(List<UUID> ids) {
        return jpaRepository.findDeletedByIdInForUpdate(ids);
    }

    @Override
    public int deleteUsers(List<UUID> ids) {
        return jpaRepository.deleteByIdIn(ids);
    }
}
