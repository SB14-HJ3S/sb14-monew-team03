package com.sprint.monew.adapter.out.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.support.IntegrationTest;
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
}
