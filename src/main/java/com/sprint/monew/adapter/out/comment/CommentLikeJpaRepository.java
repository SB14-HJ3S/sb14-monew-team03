package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.domain.comment.entity.CommentLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeJpaRepository extends JpaRepository<CommentLike, UUID> {

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);
}
