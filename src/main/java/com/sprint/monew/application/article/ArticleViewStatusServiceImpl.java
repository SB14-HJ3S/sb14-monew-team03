package com.sprint.monew.application.article;

import com.sprint.monew.application.article.required.ArticleViewStatusRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleViewStatusServiceImpl implements ArticleViewStatusService {

    private final ArticleViewStatusRepositoryPort repository;

    public Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return Set.of();
        }
        return repository.findViewedArticleIds(userId, articleIds);
    }

    @Override
    @Transactional
    public boolean register(User user, Article article) {
        LocalDateTime viewedAt = LocalDateTime.now();
        boolean inserted = repository.insertIfAbsent(user.getId(), article.getId(), viewedAt);
        if (!inserted) {
            repository.updateLastViewedAt(user.getId(), article.getId(), viewedAt);
        }
        return inserted;
    }
}
