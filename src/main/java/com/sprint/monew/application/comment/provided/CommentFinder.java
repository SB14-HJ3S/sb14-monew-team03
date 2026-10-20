package com.sprint.monew.application.comment.provided;

import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.global.pagination.PageResult;
import java.util.UUID;

public interface CommentFinder {
    PageResult<Comment> findByQuery(CommentQuery query);

    Comment findById(UUID commentId);
}
