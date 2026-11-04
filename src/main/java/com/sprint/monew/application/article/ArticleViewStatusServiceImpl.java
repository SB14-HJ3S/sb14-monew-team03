package com.sprint.monew.application.article;

import com.sprint.monew.application.article.required.ArticleViewStatusRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public boolean register(User user, Article article) {
        Optional<ArticleViewStatus> existing =
                repository.findByUserIdAndArticleId(user.getId(), article.getId());

        LocalDateTime viewedAt = LocalDateTime.now();
        if (existing.isPresent()) { // 이미 본 기사
            existing.get().viewArticle(viewedAt);
            return false;
        }

        ArticleViewStatus articleViewStatus = ArticleViewStatus.create(user, article, viewedAt);
        repository.save(articleViewStatus);
        return true;
    }
}
