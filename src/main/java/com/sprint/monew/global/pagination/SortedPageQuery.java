package com.sprint.monew.global.pagination;

public interface SortedPageQuery<T> extends PageQuery {
    // 쿼리 결과 정렬 기준
    SortOrder<T> orderBy();

    SortDirection direction();
}
