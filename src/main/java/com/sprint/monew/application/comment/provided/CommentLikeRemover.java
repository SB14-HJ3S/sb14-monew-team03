package com.sprint.monew.application.comment.provided;

import java.util.UUID;

public interface CommentLikeRemover {
    void unlike(UUID requestId, UUID commentId);
}
