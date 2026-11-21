package com.sprint.monew.domain.user.entity;

import com.sprint.monew.global.entity.BaseSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseSoftDeletableEntity {

    public static final int NICKNAME_MAX_LENGTH = 20; // 닉네임 최대 글자 수

    // @Email
    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = NICKNAME_MAX_LENGTH)
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
