package com.sprint.monew.adapter.in.controller.user.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {

    String message() default "비밀번호는 6~20자이며 영문, 숫자, 특수문자를 모두 포함해야 합니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
