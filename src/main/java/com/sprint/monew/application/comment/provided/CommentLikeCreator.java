package com.sprint.monew.application.comment.provided;

import com.sprint.monew.domain.comment.entity.Comment;
import java.util.UUID;

public interface CommentLikeCreator {
    Comment like(UUID commentId, UUID requestId);
}
