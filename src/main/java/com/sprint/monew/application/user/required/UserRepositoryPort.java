package com.sprint.monew.application.user.required;

import com.sprint.monew.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User entity);

    Optional<User> findById(UUID id);

    List<User> findAll();

    void deleteById(UUID id);

    // 사용자 도메인 전용 메서드
    Optional<User> findActiveById(UUID id); // 탈퇴하지 않는 사용자만 찾기

    Optional<User> findActiveByEmail(String email); // 로그인용

    boolean existsByEmail(String email); // 회원가입 중복확인용 - 탈퇴 사용자의 이메일도 포함

    List<User> findAllActiveByIds(List<UUID> ids);
}
