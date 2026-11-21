package com.sprint.monew.application.comment.event;

import java.util.UUID;

public record CommentDeletedEvent(UUID commentId) {}
