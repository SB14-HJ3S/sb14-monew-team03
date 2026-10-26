package com.sprint.monew.global.pagination;

public interface SortOrder<T> {
    String getValue();

    String cursorToString(T cursor);
}
