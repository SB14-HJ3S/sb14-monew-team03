package com.sprint.monew.application.comment.provided;

import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.global.pagination.PageResult;
import java.util.UUID;

public interface CommentUseCase {
    Comment register(UUID userId, UUID articleId, String content);

    PageResult<Comment> findByQuery(CommentQuery query);

    Comment findById(UUID commentId);

    Comment like(UUID commentId, UUID requestId);

    void unlike(UUID requestId, UUID commentId);

    Comment changeContent(UUID requestId, UUID commentId, String content);

    void softDeleteComment(UUID commentId);

    void hardDeleteComment(UUID commentId);
}
