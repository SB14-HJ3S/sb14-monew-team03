package com.sprint.monew.adapter.out.repository.comment;

import com.sprint.monew.domain.comment.entity.Comment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentJpaRepository extends JpaRepository<Comment, UUID>, CommentQueryRepository {

    // 동시에 누르면 숫자가 덮어써져서 사라질 수 있어서, DB에서 바로 +1 한다
    // flush: 쿼리 전에 아직 안 보낸 저장을 먼저 보낸다 / clear: 쿼리 후 옛날 숫자를 들고 있는 엔티티를 비운다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    int increaseLikeCount(@Param("id") UUID id);

    // DB에서 바로 -1 한다. 0보다 작아지지는 않는다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE Comment c SET c.likeCount = c.likeCount - 1"
                    + " WHERE c.id = :id AND c.likeCount > 0")
    int decreaseLikeCount(@Param("id") UUID id);

    // 살아 있을 때만 삭제 시각을 넣는다. 이 UPDATE가 댓글 줄을 잠가서, 동시에 지워도 한 요청만 1을 받는다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE Comment c SET c.deletedAt = :deletedAt"
                    + " WHERE c.id = :id AND c.deletedAt IS NULL")
    int softDeleteIfActive(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    // deleteById는 읽어 둔 댓글을 지우다가 이미 없으면 예외가 나서, DELETE 한 번으로 지우고 지운 줄 수만 받는다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.id = :id")
    int hardDeleteById(@Param("id") UUID id);

    // 기사 물리 삭제 때 같이 지워질 댓글을 찾는다. 논리 삭제된 댓글도 지워지니 deletedAt 조건을 넣지 않는다
    @Query("SELECT c.id FROM Comment c WHERE c.article.id = :articleId")
    List<UUID> findIdsByArticleId(@Param("articleId") UUID articleId);

    // 탈퇴자가 누른 좋아요 수만큼 댓글마다 한 번에 뺌. 0 아래로는 안 내려감
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            UPDATE Comment c
            SET c.likeCount = greatest(
                0L,
                c.likeCount - (
                    SELECT COUNT(l)
                    FROM CommentLike l
                    WHERE l.comment.id = c.id
                      AND l.user.id IN :userIds
                )
            )
            WHERE c.id IN (
                SELECT l.comment.id
                FROM CommentLike l
                WHERE l.user.id IN :userIds
            )
            """)
    int decreaseLikeCountsByUserIds(@Param("userIds") List<UUID> userIds);
}
