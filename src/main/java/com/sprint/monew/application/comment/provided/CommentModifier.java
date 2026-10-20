package com.sprint.monew.application.comment.provided;

import com.sprint.monew.domain.comment.entity.Comment;
import java.util.UUID;

public interface CommentModifier {
    Comment changeContent(UUID requestId, UUID commentId);
}
