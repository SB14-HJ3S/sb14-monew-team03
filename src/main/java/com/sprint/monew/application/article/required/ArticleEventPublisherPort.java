package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.event.ArticleSoftDeletedEvent;
import com.sprint.monew.application.article.event.ArticleViewedEvent;
import com.sprint.monew.application.article.event.ArticlesSavedEvent;

public interface ArticleEventPublisherPort {
    void publish(ArticlesSavedEvent event);

    void publish(ArticleViewedEvent event);

    void publish(ArticleSoftDeletedEvent event);
}
