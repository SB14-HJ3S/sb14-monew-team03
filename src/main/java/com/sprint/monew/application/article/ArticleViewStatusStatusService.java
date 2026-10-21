package com.sprint.monew.application.article;

import com.sprint.monew.application.article.required.ArticleViewStatusRepositoryPort;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleViewStatusStatusService implements IArticleViewStatusService {

    private final ArticleViewStatusRepositoryPort repository;

    public Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds) {
        if (articleIds.isEmpty()) return Set.of();
        return repository.findViewedArticleIds(userId, articleIds);
    }
}
