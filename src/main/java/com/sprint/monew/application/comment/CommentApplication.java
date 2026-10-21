package com.sprint.monew.application.comment;

import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.application.comment.required.CommentRepositoryPort;
import com.sprint.monew.domain.comment.entity.Comment;
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
    public Comment like(UUID commentId, UUID requestId) {
        return null;
    }

    @Override
    public void unlike(UUID commentId, UUID requestId) {}

    @Override
    @Transactional
    public CommentResult changeContent(UUID commentId, UUID requestId, String content) {
        Comment comment = getComment(commentId);

        comment.changeContent(requestId, content);

        boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, requestId);

        return new CommentResult(
                comment.getId(),
                comment.getArticle().getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getContent(),
                comment.getLikeCount(),
                likedByMe,
                comment.getCreatedAt());
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
}
