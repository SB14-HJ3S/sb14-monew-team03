package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.domain.comment.entity.CommentLike;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentLikeRepositoryAdapter implements CommentLikeRepositoryPort {

    private final CommentLikeJpaRepository jpaRepository;

    @Override
    public CommentLike save(CommentLike entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<CommentLike> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<CommentLike> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void delete(CommentLike entity) {
        jpaRepository.delete(entity);
    }

    @Override
    public boolean existsByCommentIdAndUserId(UUID commentId, UUID userId) {
        return jpaRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    @Override
    public Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId) {
        return jpaRepository.findByCommentIdAndUserId(commentId, userId);
    }
}
