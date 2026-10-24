package com.sprint.monew.global.exception.errortype;

import com.sprint.monew.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

// 사용자 도메인 에러 — 필요한 에러는 담당자가 상수로 추가
@Getter
@RequiredArgsConstructor
public enum UserErrorType implements ErrorType {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, Level.WARN, "사용자 정보를 찾을 수 없습니다"),
    USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, Level.WARN, "이미 사용 중인 이메일입니다"),
    USER_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, Level.WARN, "이메일 또는 비밀번호가 일치하지 않습니다");

    private final HttpStatus status;
    private final Level level;
    private final String message;
}
