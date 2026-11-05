package com.sprint.monew.adapter.out.article;

import com.sprint.monew.domain.article.entity.Article;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleJpaRepository extends JpaRepository<Article, UUID>, ArticleQueryRepository {
    Optional<Article> findBySourceUrl(String sourceUrl);

    List<Article> findAllBySourceUrlIn(Collection<String> sourceUrls);

    // 동시에 댓글이 달리면 숫자가 덮어써져서 사라질 수 있어서, DB에서 바로 +1 한다
    // flush: 쿼리 전에 아직 안 보낸 저장을 먼저 보낸다 / clear: 쿼리 후 옛날 숫자를 들고 있는 엔티티를 비운다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Article a SET a.commentCount = a.commentCount + 1 WHERE a.id = :id")
    int increaseCommentCount(@Param("id") UUID id);

    // DB에서 바로 -1 한다. 0보다 작아지지는 않는다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE Article a SET a.commentCount = a.commentCount - 1"
                    + " WHERE a.id = :id AND a.commentCount > 0")
    int decreaseCommentCount(@Param("id") UUID id);

    // 탈퇴자가 쓴 댓글 수만큼 기사마다 한 번에 뺌. 논리 삭제된 댓글은 그때 이미 빠져서 안 셈
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            UPDATE Article a
            SET a.commentCount = greatest(
                0,
                a.commentCount - (
                    SELECT COUNT(c)
                    FROM Comment c
                    WHERE c.article.id = a.id
                      AND c.user.id IN :userIds
                      AND c.deletedAt IS NULL
                )
            )
            WHERE a.id IN (
                SELECT c.article.id
                FROM Comment c
                WHERE c.user.id IN :userIds
                  AND c.deletedAt IS NULL
            )
            """)
    int decreaseCommentCountsByUserIds(@Param("userIds") List<UUID> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE Article a SET a.deletedAt = :deletedAt"
                    + " WHERE a.id = :id AND a.deletedAt IS NULL")
    int softDeleteIfActive(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Query(
            """
            SELECT a
            FROM Article a
            WHERE a.deletedAt IS NULL
              AND a.createdAt >= :from
              AND a.createdAt < :to
              AND (:afterId IS NULL OR a.id > :afterId)
            ORDER BY a.id ASC
            """)
    List<Article> findBackupChunk(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("afterId") UUID afterId,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            UPDATE Article a
                    SET a.viewCount = a.viewCount + 1
                    WHERE a.id = :id
                            AND a.deletedAt IS NULL
            """)
    int increaseViewCount(@Param("id") UUID id);

    @Query("SELECT a.id FROM Article a WHERE a.id IN :ids")
    Set<UUID> findExistingIds(@Param("ids") Collection<UUID> ids);
}
