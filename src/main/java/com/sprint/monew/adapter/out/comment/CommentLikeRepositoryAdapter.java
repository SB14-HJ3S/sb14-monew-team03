package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentLikeRepositoryAdapter implements CommentLikeRepositoryPort {

    private static final String UNIQUE_VIOLATION = "23505"; // DB 중복 값 오류 번호 (PostgreSQL)

    private final CommentLikeJpaRepository jpaRepository;

    // 동시에 누르면 중복 검사를 뚫을 수 있어서 DB에서 막히면 중복 에러로 바꾼다.
    // 중복이 아닌 다른 DB 오류(댓글·사용자가 지워짐 등)는 바꾸지 않고 그대로 던진다
    @Override
    public CommentLike save(CommentLike entity) {
        try {
            return jpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                throw new MonewException(CommentErrorType.COMMENT_LIKE_DUPLICATED, e);
            }
            throw e;
        }
    }

    // 원인을 따라가며 중복 값 오류(23505)인지 본다 — GlobalExceptionHandler와 같은 기준
    private boolean isUniqueViolation(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
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

    @Override
    public int deleteAllByCommentId(UUID commentId) {
        return 0;
    }
}
