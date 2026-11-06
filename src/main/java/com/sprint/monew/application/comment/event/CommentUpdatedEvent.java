package com.sprint.monew.application.comment.event;

import java.util.UUID;

public record CommentUpdatedEvent(UUID commentId, String content) {}
