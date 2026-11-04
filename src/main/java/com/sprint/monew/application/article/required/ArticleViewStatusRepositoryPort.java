package com.sprint.monew.application.article.required;

import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ArticleViewStatusRepositoryPort {

    // JPA 기본 메서드
    ArticleViewStatus save(ArticleViewStatus entity);

    Optional<ArticleViewStatus> findById(UUID id);

    List<ArticleViewStatus> findAll();

    void deleteById(UUID id);

    // 커스텀
    Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds);

    // 새 기록이 저장됐으면 true, 기존 기록이 있으면 false
    boolean insertIfAbsent(UUID userId, UUID articleId, LocalDateTime viewedAt);

    void updateLastViewedAt(UUID userId, UUID articleId, LocalDateTime viewedAt);

    Optional<ArticleViewStatus> findByUserIdAndArticleId(UUID userId, UUID articleId);
}
