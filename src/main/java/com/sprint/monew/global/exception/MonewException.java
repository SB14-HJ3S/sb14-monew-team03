package com.sprint.monew.global.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 프로젝트에서 직접 던지는 예외는 이것 하나 — 어떤 에러인지는 ErrorType 상수로 구분한다.
 *
 * <pre>{@code
 * throw new MonewException(UserErrorType.USER_NOT_FOUND);
 * throw new MonewException(UserErrorType.USER_NOT_FOUND, Map.of("userId", userId)); // 응답 details에 추가 정보
 * throw new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED, e); // 외부 라이브러리 예외 e를 원인으로 담기
 * }</pre>
 */
@Getter
public class MonewException extends RuntimeException {

    private final ErrorType errorType;

    private final Map<String, Object> details; // 어떤 값 때문에 실패했는지 등 추가 정보 (응답의 details)

    public MonewException(ErrorType errorType) {
        this(errorType, Map.of());
    }

    public MonewException(ErrorType errorType, Throwable cause) {
        this(errorType, Map.of(), cause);
    }

    public MonewException(ErrorType errorType, Map<String, Object> details) {
        this(errorType, details, null);
    }

    public MonewException(ErrorType errorType, Map<String, Object> details, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
        this.details =
                details == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }
}
