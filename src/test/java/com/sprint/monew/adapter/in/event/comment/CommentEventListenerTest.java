package com.sprint.monew.adapter.in.event.comment;

import static org.mockito.Mockito.verify;

import com.sprint.monew.application.article.event.ArticleHardDeletingEvent;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.user.event.UserHardDeletingEvent;
import java.util.List;
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

    @Test
    @DisplayName("사용자 완전 삭제 이벤트를 받으면 탈퇴자들의 좋아요 수·기사 댓글 수 정리를 부른다")
    void onUserHardDeleting() {
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        listener.onUserHardDeleting(new UserHardDeletingEvent(userIds));

        verify(commentUseCase).cleanupCountsBeforeUserHardDeletion(userIds);
    }
}
