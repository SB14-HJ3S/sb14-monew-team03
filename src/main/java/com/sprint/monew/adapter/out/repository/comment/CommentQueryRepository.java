package com.sprint.monew.adapter.out.repository.comment;

import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import java.util.List;

public interface CommentQueryRepository {

    List<Comment> findByQuery(CommentQuery query, Comment cursorComment);

    long countByQuery(CommentQuery query);
}
