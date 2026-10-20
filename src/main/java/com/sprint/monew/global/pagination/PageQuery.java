package com.sprint.monew.global.pagination;

import java.time.Instant;

public interface PageQuery {
    String cursor();

    Instant after();

    int limit();
}
