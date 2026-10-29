package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.domain.comment.entity.CommentLike;
import java.util.Optional;
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
}
