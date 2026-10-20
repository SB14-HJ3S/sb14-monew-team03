package com.sprint.monew.global.pagination;

public interface SortedPageQuery<S extends SortOrder> extends PageQuery {
    S orderBy();

    SortDirection direction();
}
