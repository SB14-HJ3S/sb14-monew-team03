package com.sprint.monew.application.article;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface IArticleViewStatusService {
    Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds);
}
