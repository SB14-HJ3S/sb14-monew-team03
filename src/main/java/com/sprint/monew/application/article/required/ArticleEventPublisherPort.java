package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;

public interface ArticleEventPublisherPort {
    void publish(ArticlesSavedEvent event);
}
