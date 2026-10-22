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

    void delete(CommentLike entity);

    // 요청자의 좋아요 여부 조회용
    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    // 좋아요 취소 시 요청자가 누른 좋아요 조회용
    Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);
}
