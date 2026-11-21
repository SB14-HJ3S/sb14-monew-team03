package com.sprint.monew.global.pagination;

import java.time.Instant;

public interface PageQuery {
    // 쿼리 결과 중 어느 요소부터 반환해야 하는지 찾는 기준
    String cursor();

    // 메인 커서로 찾은 요소가 여러 개일 때 쓸 보조 커서
    Instant after();

    // 페이지 1회 크기
    int limit();
}
