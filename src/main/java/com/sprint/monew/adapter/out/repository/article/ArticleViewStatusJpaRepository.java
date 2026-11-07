package com.sprint.monew.adapter.out.repository.article;

import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewStatusJpaRepository extends JpaRepository<ArticleViewStatus, UUID> {
    @Query(
            """
            select v.article.id
            from ArticleViewStatus v
            where v.user.id = :userId
              and v.article.id in :articleIds
            """)
    Set<UUID> findViewedArticleIds(
            @Param("userId") UUID userId, @Param("articleIds") Collection<UUID> articleIds);

    Optional<ArticleViewStatus> findByUserIdAndArticleId(UUID userId, UUID articleId);

    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
                INSERT INTO article_view_statuses
                    (id, created_at, user_id, article_id, last_viewed_at)
                VALUES (:id, :createdAt, :userId, :articleId, :viewedAt)
                ON CONFLICT (user_id, article_id) DO NOTHING
                """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("createdAt") Instant createdAt,
            @Param("userId") UUID userId,
            @Param("articleId") UUID articleId,
            @Param("viewedAt") LocalDateTime viewedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
                UPDATE ArticleViewStatus v
                SET v.lastViewedAt = CASE
                    WHEN v.lastViewedAt < :viewedAt THEN :viewedAt
                    ELSE v.lastViewedAt
                END
                WHERE v.user.id = :userId AND v.article.id = :articleId
            """)
    int updateLastViewedAt(
            @Param("userId") UUID userId,
            @Param("articleId") UUID articleId,
            @Param("viewedAt") LocalDateTime viewedAt);
}
