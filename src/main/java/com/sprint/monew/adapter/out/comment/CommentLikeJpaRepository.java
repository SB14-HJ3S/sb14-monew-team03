package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.domain.comment.entity.CommentLike;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeJpaRepository extends JpaRepository<CommentLike, UUID> {

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

    // 좋아요를 하나씩 불러와 지우지 않고 DELETE 한 번으로 지운다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    int deleteAllByCommentId(@Param("commentId") UUID commentId);

    // 목록에 나온 댓글 중 요청자가 좋아요 누른 것만 한 번에 가져옴 (댓글마다 따로 조회 안 하게)
    @Query(
            "SELECT cl.comment.id FROM CommentLike cl"
                    + " WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    Set<UUID> findLikedCommentIds(
            @Param("userId") UUID userId, @Param("commentIds") Collection<UUID> commentIds);
}
