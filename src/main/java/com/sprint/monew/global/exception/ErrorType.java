package com.sprint.monew.global.exception;

import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

/**
 * 에러 한 종류의 규격 — 도메인별 enum(errortype/UserErrorType 등)이 이걸 구현한다.
 *
 * <p>새 에러가 필요하면 담당 도메인 enum에 상수 한 줄만 추가하면 된다.
 */
public interface ErrorType {

    String name(); // 상수 이름이 그대로 응답의 에러 코드 (예: USER_NOT_FOUND) — 도메인 접두어로 겹치지 않게

    HttpStatus getStatus(); // 응답 HTTP 상태코드 (400, 404, 409 …)

    Level getLevel(); // 서버 로그 레벨 — 사용자 실수는 WARN, 서버 문제는 ERROR

    String getMessage(); // 사용자에게 보여 줄 메시지
}
