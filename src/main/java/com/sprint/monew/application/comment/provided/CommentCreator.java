package com.sprint.monew.application.comment.provided;

import com.sprint.monew.domain.comment.entity.Comment;
import java.util.UUID;

public interface CommentCreator {
    Comment register(UUID userId, UUID articleId, String content);
}
