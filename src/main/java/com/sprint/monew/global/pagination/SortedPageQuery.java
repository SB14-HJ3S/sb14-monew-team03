package com.sprint.monew.global.pagination;

public interface SortedPageQuery<T, S extends SortOrder<T>> extends PageQuery {
    S orderBy();

    SortDirection direction();
}
