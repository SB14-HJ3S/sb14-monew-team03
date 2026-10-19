package com.sprint.monew.global.exception;

import com.sprint.monew.global.exception.errortype.CommonErrorType;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러에서 난 예외를 모두 ErrorResponse 형식으로 바꿔 응답한다.
 *
 * <p>ResponseEntityExceptionHandler를 상속해서, 스프링 기본 에러(없는 주소 404, 잘못된 JSON 등)도 원래 상태코드 그대로 같은 형식으로
 * 나간다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String UNIQUE_VIOLATION = "23505"; // DB 중복 값 오류 번호 (PostgreSQL)

    // 우리가 던진 MonewException → ErrorType의 상태코드·메시지로 응답
    @ExceptionHandler(MonewException.class)
    public ResponseEntity<ErrorResponse> handleMonewException(MonewException e) {
        ErrorType errorType = e.getErrorType();
        LoggingEventBuilder logger = log.atLevel(errorType.getLevel());
        if (errorType.getLevel() == Level.ERROR) {
            logger = logger.setCause(e);
        }
        logger.log("[{}] {} details={}", errorType.name(), errorType.getMessage(), e.getDetails());

        return ResponseEntity.status(errorType.getStatus())
                .body(
                        ErrorResponse.of(
                                errorType, e.getDetails(), e, errorType.getStatus().value()));
    }

    // @Validated 검증 실패 → 400, 틀린 값을 details에 담음
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        e.getConstraintViolations()
                .forEach(v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));

        log.warn("[{}] 입력값 검증 실패 details={}", CommonErrorType.INVALID_REQUEST.name(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.of(
                                CommonErrorType.INVALID_REQUEST,
                                details,
                                e,
                                HttpStatus.BAD_REQUEST.value()));
    }

    // 동시 요청은 서비스의 중복 검사를 통과해도 DB 유니크 제약에서 걸릴 수 있다.
    // 유니크 위반(PostgreSQL SQLState 23505)만 409, 그 외 제약 위반은 서버 버그라 500
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException e) {
        if (!isUniqueViolation(e)) {
            return handleUnexpected(e);
        }
        log.warn(
                "[{}] 유니크 제약 위반: {}",
                CommonErrorType.DUPLICATE_RESOURCE.name(),
                e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ErrorResponse.of(
                                CommonErrorType.DUPLICATE_RESOURCE,
                                Map.of(),
                                e,
                                HttpStatus.CONFLICT.value()));
    }

    private boolean isUniqueViolation(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    // 위에서 못 잡은 예상 못 한 예외 → 500 (내부 내용은 숨김)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("[{}] 예상하지 못한 예외", CommonErrorType.INTERNAL_ERROR.name(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.of(
                                CommonErrorType.INTERNAL_ERROR,
                                Map.of(),
                                e,
                                HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    // @Valid 요청 본문(JSON) 검증 실패 → 400, 틀린 필드를 details에 담음
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> details.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return invalidRequest(ex, details, headers, status);
    }

    // @RequestParam·@PathVariable 검증 실패 → 400
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        ex.getParameterValidationResults()
                .forEach(
                        result ->
                                details.putIfAbsent(
                                        result.getMethodParameter().getParameterName(),
                                        result.getResolvableErrors().get(0).getDefaultMessage()));

        return invalidRequest(ex, details, headers, status);
    }

    // 필수 파라미터 누락 → 400
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return invalidRequest(ex, Map.of(ex.getParameterName(), "필수 값입니다"), headers, status);
    }

    // 값 타입이 안 맞음(UUID 형식 오류, enum 오타 등) → 400
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return invalidRequest(ex, Map.of(ex.getPropertyName(), "형식이 올바르지 않습니다"), headers, status);
    }

    private ResponseEntity<Object> invalidRequest(
            Exception ex, Map<String, Object> details, HttpHeaders headers, HttpStatusCode status) {
        log.warn("[{}] 입력값 검증 실패 details={}", CommonErrorType.INVALID_REQUEST.name(), details);
        return ResponseEntity.status(status)
                .headers(headers)
                .body(
                        ErrorResponse.of(
                                CommonErrorType.INVALID_REQUEST, details, ex, status.value()));
    }

    // 그 밖의 스프링 기본 예외 → 상태코드는 그대로, 본문만 ErrorResponse 형식으로
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ErrorType errorType = errorTypeOf(statusCode);
        if (statusCode.is5xxServerError()) {
            log.error("[{}] 서버 오류", errorType.name(), ex);
        } else {
            log.warn("[{}] {}", errorType.name(), ex.getMessage());
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(statusCode);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.body(ErrorResponse.of(errorType, Map.of(), ex, statusCode.value()));
    }

    private ErrorType errorTypeOf(HttpStatusCode statusCode) {
        if (statusCode.is5xxServerError()) {
            return CommonErrorType.INTERNAL_ERROR;
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            return CommonErrorType.INVALID_REQUEST;
        }
        return switch (status) {
            case UNAUTHORIZED -> CommonErrorType.UNAUTHORIZED;
            case FORBIDDEN -> CommonErrorType.FORBIDDEN;
            case NOT_FOUND -> CommonErrorType.RESOURCE_NOT_FOUND;
            case METHOD_NOT_ALLOWED -> CommonErrorType.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> CommonErrorType.UNSUPPORTED_MEDIA_TYPE;
            default -> CommonErrorType.INVALID_REQUEST;
        };
    }
}
