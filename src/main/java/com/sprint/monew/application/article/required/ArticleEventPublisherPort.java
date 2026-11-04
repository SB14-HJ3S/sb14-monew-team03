package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.event.ArticleHardDeletingEvent;
import com.sprint.monew.application.article.event.ArticleSoftDeletedEvent;
import com.sprint.monew.application.article.event.ArticlesSavedEvent;

public interface ArticleEventPublisherPort {
    void publish(ArticlesSavedEvent event);

    void publish(ArticleSoftDeletedEvent event);

    void publish(ArticleHardDeletingEvent event);
}
