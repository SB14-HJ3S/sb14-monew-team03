package com.sprint.monew.global.exception.errortype;

import com.sprint.monew.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

// 특정 도메인에 속하지 않는 공통 에러 (잘못된 요청·권한·서버 오류 등)
@Getter
@RequiredArgsConstructor
public enum CommonErrorType implements ErrorType {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, Level.WARN, "잘못된 요청입니다"),
    INVALID_SORT_FIELD(HttpStatus.BAD_REQUEST, Level.WARN, "지원하지 않는 정렬 기준입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, Level.WARN, "로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, Level.WARN, "권한이 없습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, Level.WARN, "요청한 리소스를 찾을 수 없습니다"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, Level.WARN, "이미 존재하는 데이터입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, Level.WARN, "허용되지 않는 요청 방식입니다"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, Level.WARN, "지원하지 않는 요청 형식입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR, "서버 내부 오류입니다"),

    // pagination 관련
    PAGINATION_NOT_BOTH_CURSOR_AND_AFTER(
            HttpStatus.BAD_REQUEST, Level.ERROR, "cursor와 after 값이 같이 전달되어야 합니다");

    private final HttpStatus status;
    private final Level level;
    private final String message;
}
