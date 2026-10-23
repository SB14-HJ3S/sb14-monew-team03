package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentLikeRepositoryAdapter implements CommentLikeRepositoryPort {

    private final CommentLikeJpaRepository jpaRepository;

    // 동시에 누르면 중복 검사를 뚫을 수 있어서 DB에서 막히면 중복 에러로 바꾼다.
    @Override
    public CommentLike save(CommentLike entity) {
        try {
            return jpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new MonewException(CommentErrorType.COMMENT_LIKE_DUPLICATED, e);
        }
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
