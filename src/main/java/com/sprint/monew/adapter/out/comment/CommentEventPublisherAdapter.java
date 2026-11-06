package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.application.comment.event.CommentCreatedEvent;
import com.sprint.monew.application.comment.event.CommentDeletedEvent;
import com.sprint.monew.application.comment.event.CommentLikeCreatedEvent;
import com.sprint.monew.application.comment.event.CommentUnlikedEvent;
import com.sprint.monew.application.comment.event.CommentUpdatedEvent;
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

    @Override
    public void publish(CommentCreatedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(CommentUpdatedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(CommentDeletedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(CommentLikeCreatedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(CommentUnlikedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
