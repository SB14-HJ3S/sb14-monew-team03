package com.sprint.monew.application.comment.provided;

import java.util.UUID;

public interface CommentRemover {
    void softDeleteComment(UUID commentId);

    void hardDeleteComment(UUID commentId);
}
