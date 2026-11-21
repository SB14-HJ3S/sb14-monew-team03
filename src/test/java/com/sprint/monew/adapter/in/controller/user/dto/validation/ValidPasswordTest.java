package com.sprint.monew.adapter.in.controller.user.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidPasswordTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    record PasswordForm(@ValidPassword String password) {}

    @ParameterizedTest
    @ValueSource(strings = {"monew1!", "abc123!@", "Password1234567890!@"})
    @DisplayName("6~20자이고 영문·숫자·특수문자가 모두 있으면 통과한다")
    void givenValidPassword_whenValidate_thenNoViolation(String password) {
        assertThat(validator.validate(new PasswordForm(password))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab1!", "abcdefgh1", "abcdefgh!", "12345678!", "Password12345678901!@"})
    @DisplayName("길이가 6~20자를 벗어나거나 영문·숫자·특수문자 중 하나라도 없으면 password 칸 오류로 걸린다")
    void givenInvalidPassword_whenValidate_thenViolationOnPassword(String password) {
        assertThat(validator.validate(new PasswordForm(password)))
                .singleElement()
                .satisfies(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("password"));
    }

    @Test
    @DisplayName("null이면 통과하고 빈 값 검사는 @NotBlank에 맡긴다")
    void givenNullPassword_whenValidate_thenNoViolation() {
        assertThat(validator.validate(new PasswordForm(null))).isEmpty();
    }
}
