package com.sprint.monew.global.exception.errortype;

import com.sprint.monew.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

// 알림 도메인 에러 — 필요한 에러는 담당자가 상수로 추가
@Getter
@RequiredArgsConstructor
public enum NotificationErrorType implements ErrorType {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, Level.WARN, "알림을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final Level level;
    private final String message;
}
