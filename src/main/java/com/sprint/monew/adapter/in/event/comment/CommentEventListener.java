package com.sprint.monew.adapter.in.event.comment;

import com.sprint.monew.application.article.event.ArticleHardDeletingEvent;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.user.event.UserHardDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventListener {

    private final CommentUseCase commentUseCase;

    @EventListener
    public void onArticleHardDeleting(ArticleHardDeletingEvent event) {
        commentUseCase.deleteLikeNotificationsByArticle(event.articleId());
    }

    // 사용자를 지우기 직전에 같은 트랜잭션에서 받음 (아직 탈퇴자의 좋아요·댓글을 셀 수 있음)
    @EventListener
    public void onUserHardDeleting(UserHardDeletingEvent event) {}
}
