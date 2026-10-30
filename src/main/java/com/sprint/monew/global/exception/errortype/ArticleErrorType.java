package com.sprint.monew.global.exception.errortype;

import com.sprint.monew.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

// 뉴스기사 도메인 에러 — 필요한 에러는 담당자가 상수로 추가
@Getter
@RequiredArgsConstructor
public enum ArticleErrorType implements ErrorType {
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, Level.WARN, "뉴스 기사 정보를 찾을 수 없습니다"),

    // 수집한 기사 데이터 관련 오류
    ARTICLE_SOURCE_URL_MISSING(HttpStatus.NOT_FOUND, Level.WARN, "뉴스 기사의 URL이 없습니다"),
    ARTICLE_TITLE_MISSING(HttpStatus.NOT_FOUND, Level.WARN, "뉴스 기사의 제목이 없습니다"),
    ARTICLE_PUBLISHED_AT_INVALID(HttpStatus.BAD_GATEWAY, Level.ERROR, "기사 발행일이 올바르지 않습니다"),

    // 수집 및 저장 작업 관련 오류
    ARTICLE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR, "수집한 기사 저장에 실패했습니다"),
    ARTICLE_COLLECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR, "관심사 기사 수집에 실패했습니다");

    private final HttpStatus status;
    private final Level level;
    private final String message;
}
