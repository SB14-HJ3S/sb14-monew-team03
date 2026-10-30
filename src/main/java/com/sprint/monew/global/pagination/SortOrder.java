package com.sprint.monew.global.pagination;

public interface SortOrder<T> {
    // 클라이언트 요청을 ENUM으로 매핑할 때 사용되는 값
    String getValue();

    // 쿼리 결과 반환 시, 다음 페이지의 cursor
    String extractNextCursorFromLastEntity(T lastEntity);
}
