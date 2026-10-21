package com.sprint.monew.application.comment.required;

import com.sprint.monew.domain.comment.entity.CommentLike;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepositoryPort {
    // JPA 기본 메서드
    CommentLike save(CommentLike entity);

    Optional<CommentLike> findById(UUID id);

    List<CommentLike> findAll();

    void deleteById(UUID id);

    // 본인 쓸 거
    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);
}
