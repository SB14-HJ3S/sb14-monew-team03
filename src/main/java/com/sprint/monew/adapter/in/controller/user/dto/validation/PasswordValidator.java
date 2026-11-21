package com.sprint.monew.adapter.in.controller.user.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 20;
    private static final Pattern PATTERN =
            Pattern.compile(
                    "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.length() >= MIN_LENGTH
                && value.length() <= MAX_LENGTH
                && PATTERN.matcher(value).matches();
    }
}
