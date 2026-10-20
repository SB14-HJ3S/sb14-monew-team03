package com.sprint.monew.domain.user.entity;

import com.sprint.monew.global.entity.BaseSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// JPA Auditing은 엔티티가 하나도 없으면 앱이 뜨지 않아서 미리 둔 빈 엔티티 — 필드는 사용자 담당자가 채운다
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseSoftDeletableEntity {
    // @Email
    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false)
    // 영문, 숫자, 특수문자 6자 이상 <- validation 만들기
    private String password;

    private User(String email, String nickname, String password) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
    }

    public static User create(String email, String nickname, String password) {
        return new User(email, nickname, password);
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}
