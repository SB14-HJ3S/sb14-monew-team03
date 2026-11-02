package com.sprint.monew.application.comment;

import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.application.comment.required.CommentRepositoryPort;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

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
    @Transactional
    public Comment saveComment(Comment comment) {
        return commentRepository.save(comment);
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

    // 읽은 뒤에 다른 요청이 먼저 지웠으면 없는 댓글로 본다
    @Override
    @Transactional
    public void softDelete(UUID commentId) {
        if (commentRepository.softDeleteIfActive(commentId, Instant.now()) == 0) {
            throw new MonewException(CommentErrorType.COMMENT_NOT_FOUND);
        }
    }

    // 지우기 전에 살아 있던 댓글이었으면 true를 돌려준다 (댓글 수를 줄일지 판단용)
    @Override
    @Transactional
    public boolean hardDelete(UUID commentId) {
        // 살아 있었는지는 읽은 값 말고 DB에서 조건부로 지워 보고 판단한다
        // 동시에 온 논리 삭제와 겹쳐도 둘 중 한 요청만 true를 받는다
        boolean wasActive = commentRepository.softDeleteIfActive(commentId, Instant.now()) == 1;

        // 좋아요가 댓글을 가리키고 있어서 좋아요부터 지운다
        commentLikeRepository.deleteAllByCommentId(commentId);

        // 다른 물리 삭제 요청이 먼저 지웠으면 없는 댓글로 본다
        if (commentRepository.hardDeleteById(commentId) == 0) {
            throw new MonewException(CommentErrorType.COMMENT_NOT_FOUND);
        }
        return wasActive;
    }

    @Override
    public List<UUID> findIdsByArticleId(UUID articleId) {
        return List.of();
    }
}
