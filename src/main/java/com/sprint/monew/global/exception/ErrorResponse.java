package com.sprint.monew.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatusCode;

// 모든 에러 응답의 공통 형식 — GlobalExceptionHandler가 예외를 이 형식으로 바꿔 내려준다
public record ErrorResponse(
        @Schema(description = "에러 발생 시각", example = "2026-11-01T12:00:00Z") Instant timestamp,
        @Schema(description = "에러 코드") String code,
        @Schema(description = "에러 메시지") String message,
        @Schema(description = "에러 상세 정보") Map<String, Object> details,
        @Schema(description = "예외 종류") String exceptionType,
        @Schema(description = "HTTP 상태 코드") int status) {

    public static ErrorResponse of(
            ErrorType errorType, Map<String, Object> details, Exception exception, int status) {
        return new ErrorResponse(
                Instant.now(),
                errorType.name(),
                errorType.getMessage(),
                details,
                exceptionTypeOf(exception, status),
                status);
    }

    // 예상 못 한 500 에러는 내부 예외 이름을 숨기고 "Exception"으로 내보낸다
    private static String exceptionTypeOf(Exception exception, int status) {
        if (HttpStatusCode.valueOf(status).is5xxServerError()
                && !(exception instanceof MonewException)) {
            return Exception.class.getSimpleName();
        }
        return exception.getClass().getSimpleName();
    }
}
