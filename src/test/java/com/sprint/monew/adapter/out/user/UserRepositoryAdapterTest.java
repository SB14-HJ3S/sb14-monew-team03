package com.sprint.monew.adapter.out.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.support.IntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class UserRepositoryAdapterTest {

    @Autowired private UserRepositoryAdapter userRepository;

    @Test
    @DisplayName("탈퇴하지 않은 사용자는 조회된다")
    void givenActiveUser_whenFindActiveById_thenFound() {
        User saved = userRepository.save(User.create("active@monew.com", "활성", "password1!"));

        assertThat(userRepository.findActiveById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("탈퇴(논리 삭제)한 사용자는 조회되지 않는다")
    void givenDeletedUser_whenFindActiveById_thenEmpty() {
        User saved = userRepository.save(User.create("deleted@monew.com", "탈퇴", "password1!"));
        saved.softDelete();

        assertThat(userRepository.findActiveById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("탈퇴하지 않은 사용자는 이메일로 조회된다")
    void givenActiveUser_whenFindActiveByEmail_thenFound() {
        userRepository.save(User.create("hello@monew.com", "활성", "password1!"));

        assertThat(userRepository.findActiveByEmail("hello@monew.com")).isPresent();
    }

    @Test
    @DisplayName("탈퇴(논리 삭제)한 사용자는 이메일로도 조회되지 않는다 — 로그인 차단")
    void givenDeletedUser_whenFindActiveByEmail_thenEmpty() {
        User saved = userRepository.save(User.create("bye@monew.com", "탈퇴", "password1!"));
        saved.softDelete();

        assertThat(userRepository.findActiveByEmail("bye@monew.com")).isEmpty();
    }

    @Test
    @DisplayName("탈퇴한 사용자의 이메일도 이미 있는 이메일로 본다 — 재가입 중복 확인")
    void givenDeletedUser_whenExistsByEmail_thenTrue() {
        User saved = userRepository.save(User.create("left@monew.com", "탈퇴", "password1!"));
        saved.softDelete();

        assertThat(userRepository.existsByEmail("left@monew.com")).isTrue();
    }

    @Test
    @DisplayName("여러 id로 조회하면 탈퇴했거나 없는 사용자는 빼고 활성 사용자만 찾는다")
    void givenActiveDeletedAndMissingIds_whenFindAllActiveByIds_thenOnlyActive() {
        User first = userRepository.save(User.create("first@monew.com", "첫째", "password1!"));
        User second = userRepository.save(User.create("second@monew.com", "둘째", "password1!"));
        User deleted = userRepository.save(User.create("gone@monew.com", "탈퇴", "password1!"));
        deleted.softDelete();

        List<User> result =
                userRepository.findAllActiveByIds(
                        List.of(first.getId(), second.getId(), deleted.getId(), UUID.randomUUID()));

        assertThat(result).containsExactlyInAnyOrder(first, second);
    }
}
