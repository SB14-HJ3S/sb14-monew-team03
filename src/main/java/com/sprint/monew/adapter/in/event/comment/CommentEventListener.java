package com.sprint.monew.adapter.in.event.comment;

import com.sprint.monew.application.article.event.ArticleHardDeletingEvent;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventListener {

    private final CommentUseCase commentUseCase;

    @EventListener
    public void onArticleHardDeleting(ArticleHardDeletingEvent event) {}
}
