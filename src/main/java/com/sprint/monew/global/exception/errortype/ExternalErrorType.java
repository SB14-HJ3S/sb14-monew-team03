package com.sprint.monew.global.exception.errortype;

import com.sprint.monew.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

/**
 * 외부 서비스(네이버 뉴스 API·RSS·S3) 호출 실패.
 *
 * <p>외부 라이브러리 예외는 외부를 부르는 클래스 안에서 잡아 {@code throw new MonewException(ExternalErrorType.XXX, e)}로 바꿔
 * 던진다. 메시지는 화면에 그대로 뜨므로 API 키·버킷 이름 같은 내부 정보는 넣지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum ExternalErrorType implements ErrorType {
    EXTERNAL_API_CALL_FAILED(
            HttpStatus.BAD_GATEWAY, Level.ERROR, "외부 서비스 호출에 실패했습니다. 잠시 후 다시 시도해 주세요");

    private final HttpStatus status;
    private final Level level;
    private final String message;
}
