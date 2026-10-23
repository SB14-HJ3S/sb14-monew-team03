package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.application.comment.required.CommentEventPublisherPort;
import com.sprint.monew.domain.comment.event.CommentLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisherAdapter implements CommentEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(CommentLikedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
