package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleEventPublisherAdapter implements ArticleEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(ArticlesSavedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
