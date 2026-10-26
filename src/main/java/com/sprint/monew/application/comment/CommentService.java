package com.sprint.monew.application.comment;

import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.application.comment.required.CommentRepositoryPort;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService implements ICommentService {

    private final CommentRepositoryPort commentRepository;
    private final CommentLikeRepositoryPort commentLikeRepository;

    @Override
    public Comment getComment(UUID commentId) {
        return commentRepository
                .findById(commentId)
                .orElseThrow(() -> new MonewException(CommentErrorType.COMMENT_NOT_FOUND));
    }

    // 논리 삭제된 댓글은 없는 댓글처럼 다룬다
    @Override
    public Comment getActiveComment(UUID commentId) {
        Comment comment = getComment(commentId);
        if (comment.isDeleted()) {
            throw new MonewException(CommentErrorType.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    @Override
    public boolean isLikedBy(UUID commentId, UUID userId) {
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    @Override
    @Transactional
    public CommentLike saveLike(User user, Comment comment) {
        if (isLikedBy(comment.getId(), user.getId())) {
            throw new MonewException(CommentErrorType.COMMENT_LIKE_DUPLICATED);
        }
        return commentLikeRepository.save(CommentLike.create(user, comment));
    }

    @Override
    @Transactional
    public void deleteLike(UUID commentId, UUID userId) {
        CommentLike commentLike =
                commentLikeRepository
                        .findByCommentIdAndUserId(commentId, userId)
                        .orElseThrow(
                                () -> new MonewException(CommentErrorType.COMMENT_LIKE_NOT_FOUND));
        commentLikeRepository.delete(commentLike);
    }

    @Override
    @Transactional
    public void increaseLikeCount(UUID commentId) {
        commentRepository.increaseLikeCount(commentId);
    }

    @Override
    @Transactional
    public void decreaseLikeCount(UUID commentId) {
        commentRepository.decreaseLikeCount(commentId);
    }
}
