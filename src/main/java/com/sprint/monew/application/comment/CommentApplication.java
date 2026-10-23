package com.sprint.monew.application.comment;

import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.application.comment.required.CommentEventPublisherPort;
import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.application.comment.required.CommentRepositoryPort;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.comment.event.CommentLikedEvent;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import com.sprint.monew.global.pagination.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentApplication implements CommentUseCase {

    private final CommentRepositoryPort commentRepository;
    private final CommentLikeRepositoryPort commentLikeRepository;
    private final UserUseCase userUseCase;
    private final CommentEventPublisherPort commentEventPublisher;

    @Override
    public Comment register(UUID userId, UUID articleId, String content) {
        return null;
    }

    @Override
    public PageResult<Comment> findByQuery(CommentQuery query) {
        return null;
    }

    @Override
    public Comment findById(UUID commentId) {
        return null;
    }

    @Override
    @Transactional
    public CommentLikeResult like(UUID commentId) {
        UUID requestUserId = RequestIdContext.get();
        User user = userUseCase.getActiveUser(requestUserId);
        Comment comment = getActiveComment(commentId);
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId)) {
            throw new MonewException(CommentErrorType.COMMENT_LIKE_DUPLICATED);
        }

        CommentLike commentLike = commentLikeRepository.save(CommentLike.create(user, comment));

        // +1 쿼리 뒤에는 댓글 정보를 못 읽을 수 있어서 알림 이벤트를 먼저 보낸다
        commentEventPublisher.publish(
                new CommentLikedEvent(
                        comment.getId(),
                        comment.getUser().getId(),
                        user.getId(),
                        user.getNickname()));

        commentRepository.increaseLikeCount(commentId);

        // 늘어난 좋아요 수를 응답에 넣으려고 댓글을 다시 읽는다
        return CommentLikeResult.of(commentLike, getComment(commentId));
    }

    @Override
    @Transactional
    public void unlike(UUID commentId) {
        UUID requestUserId = RequestIdContext.get();
        getActiveComment(commentId);

        CommentLike commentLike =
                commentLikeRepository
                        .findByCommentIdAndUserId(commentId, requestUserId)
                        .orElseThrow(
                                () -> new MonewException(CommentErrorType.COMMENT_LIKE_NOT_FOUND));

        commentLikeRepository.delete(commentLike);
        commentRepository.decreaseLikeCount(commentId);
    }

    @Override
    @Transactional
    public CommentResult changeContent(UUID commentId, String content) {
        UUID requestUserId = RequestIdContext.get();
        Comment comment = getComment(commentId);

        comment.changeContent(requestUserId, content);

        boolean likedByMe =
                commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId);

        return CommentResult.of(comment, likedByMe);
    }

    @Override
    public void softDeleteComment(UUID commentId) {}

    @Override
    public void hardDeleteComment(UUID commentId) {}

    private Comment getComment(UUID commentId) {
        return commentRepository
                .findById(commentId)
                .orElseThrow(() -> new MonewException(CommentErrorType.COMMENT_NOT_FOUND));
    }

    // 논리 삭제된 댓글은 없는 댓글처럼 다룬다
    private Comment getActiveComment(UUID commentId) {
        Comment comment = getComment(commentId);
        if (comment.isDeleted()) {
            throw new MonewException(CommentErrorType.COMMENT_NOT_FOUND);
        }
        return comment;
    }
}
