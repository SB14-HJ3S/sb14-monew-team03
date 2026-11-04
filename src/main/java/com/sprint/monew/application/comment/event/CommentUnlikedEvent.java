package com.sprint.monew.application.comment.event;

import java.util.UUID;

public record CommentUnlikedEvent(UUID commentId, UUID likedById, long commentLikeCount) {}
