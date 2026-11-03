package com.sprint.monew.adapter.in.event.comment;

import static org.mockito.Mockito.verify;

import com.sprint.monew.application.article.event.ArticleHardDeletingEvent;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentEventListenerTest {

    @Mock private CommentUseCase commentUseCase;
    @InjectMocks private CommentEventListener listener;

    @Test
    @DisplayName("기사 물리 삭제 이벤트를 받으면 그 기사 댓글들의 좋아요 알림을 지운다")
    void onArticleHardDeleting() {
        UUID articleId = UUID.randomUUID();

        listener.onArticleHardDeleting(new ArticleHardDeletingEvent(articleId));

        verify(commentUseCase).deleteLikeNotificationsByArticle(articleId);
    }
}
