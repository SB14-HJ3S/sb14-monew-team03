package com.sprint.monew.application.comment.required;

import com.sprint.monew.application.comment.event.CommentCreatedEvent;
import com.sprint.monew.application.comment.event.CommentDeletedEvent;
import com.sprint.monew.application.comment.event.CommentLikeCreatedEvent;
import com.sprint.monew.application.comment.event.CommentUnlikedEvent;
import com.sprint.monew.application.comment.event.CommentUpdatedEvent;
import com.sprint.monew.domain.comment.event.CommentLikedEvent;

public interface CommentEventPublisherPort {

    // 댓글 좋아요 알림용 이벤트 발행
    void publish(CommentLikedEvent event);

    // ===== 활동 내역용 =====
    void publish(CommentCreatedEvent event);

    void publish(CommentUpdatedEvent event);

    void publish(CommentDeletedEvent event);

    void publish(CommentLikeCreatedEvent event);

    void publish(CommentUnlikedEvent event);
}
