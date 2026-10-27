package com.sprint.monew.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatabaseConstraintUtils {

    public static boolean isUniqueConstraintViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException constraintViolationException
                && constraintViolationException.getKind()
                        == ConstraintViolationException.ConstraintKind.UNIQUE;
    }
}
