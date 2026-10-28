package com.sprint.monew.application.comment.provided;

import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.global.pagination.PageResult;
import java.util.UUID;

public interface CommentUseCase {
    CommentResult register(UUID userId, UUID articleId, String content);

    PageResult<Comment> findByQuery(CommentQuery query);

    Comment findById(UUID commentId);

    CommentLikeResult like(UUID commentId);

    void unlike(UUID commentId);

    CommentResult changeContent(UUID commentId, String content);

    void softDeleteComment(UUID commentId);

    void hardDeleteComment(UUID commentId);
}
