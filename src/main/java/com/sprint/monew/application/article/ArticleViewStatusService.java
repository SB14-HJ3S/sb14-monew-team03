package com.sprint.monew.application.article;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface ArticleViewStatusService {
    Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds);
}
