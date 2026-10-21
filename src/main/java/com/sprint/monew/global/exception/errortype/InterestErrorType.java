package com.sprint.monew.global.exception.errortype;

import com.sprint.monew.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

// 관심사 도메인 에러 — 필요한 에러는 담당자가 상수로 추가
@Getter
@RequiredArgsConstructor
public enum InterestErrorType implements ErrorType {
    INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, Level.WARN, "관심사 정보를 찾을 수 없습니다"),
    SIMILAR_NAME_CONFLICT(HttpStatus.CONFLICT, Level.INFO, "비슷한 관심사 이름이 이미 사용중입니다."),
    ;

    private final HttpStatus status;
    private final Level level;
    private final String message;
}
