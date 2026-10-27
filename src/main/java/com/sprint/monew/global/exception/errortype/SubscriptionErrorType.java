package com.sprint.monew.global.exception.errortype;

import com.sprint.monew.global.exception.ErrorType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum SubscriptionErrorType implements ErrorType {
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, Level.INFO, "구독 정보를 찾을 수 없습니다"),
    ;

    private final HttpStatus status;
    private final Level level;
    private final String message;
}
