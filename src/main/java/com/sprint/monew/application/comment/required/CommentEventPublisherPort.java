package com.sprint.monew.application.comment.required;

import com.sprint.monew.domain.comment.event.CommentLikedEvent;

public interface CommentEventPublisherPort {

    // 댓글 좋아요 알림용 이벤트 발행
    void publish(CommentLikedEvent event);
}
