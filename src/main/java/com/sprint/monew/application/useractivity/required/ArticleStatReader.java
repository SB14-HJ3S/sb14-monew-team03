package com.sprint.monew.application.useractivity.required;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ArticleStatReader {
    /** 기사 ID → 현재 숫자. 없는 기사는 빠진다 */
    Map<UUID, ArticleStat> findStats(Collection<UUID> articleIds);

    record ArticleStat(long viewCount, long commentCount) {}
}
