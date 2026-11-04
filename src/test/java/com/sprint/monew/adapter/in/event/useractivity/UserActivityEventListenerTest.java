package com.sprint.monew.adapter.in.event.useractivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.comment.event.CommentCreatedEvent;
import com.sprint.monew.application.user.event.UserHardDeletingEvent;
import com.sprint.monew.application.useractivity.provided.UserActivityUseCase;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityEventListenerTest {

    @Mock private UserActivityUseCase userActivityUseCase;

    @InjectMocks private UserActivityEventListener listener;

    @Captor private ArgumentCaptor<CommentActivityResult> commentCaptor;

    @Test
    @DisplayName("댓글 작성 이벤트를 받으면 같은 값으로 활동 내역에 댓글을 추가한다.")
    void givenCommentCreated_whenOn_thenAddsCommentWithSameValues() {
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-04T00:00:00Z");

        listener.on(
                new CommentCreatedEvent(
                        commentId, articleId, "기사 제목", userId, "우디", "댓글 내용", 3L, createdAt));

        verify(userActivityUseCase)
                .commentCreated(org.mockito.ArgumentMatchers.eq(userId), commentCaptor.capture());
        CommentActivityResult item = commentCaptor.getValue();
        assertThat(item.id()).isEqualTo(commentId);
        assertThat(item.articleId()).isEqualTo(articleId);
        assertThat(item.articleTitle()).isEqualTo("기사 제목");
        assertThat(item.userNickname()).isEqualTo("우디");
        assertThat(item.content()).isEqualTo("댓글 내용");
        assertThat(item.likeCount()).isEqualTo(3L);
        assertThat(item.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("사용자 완전 삭제 이벤트를 받으면 ID 목록 그대로 활동 내역을 정리한다.")
    void givenUserHardDeleting_whenOn_thenUsersHardDeleted() {
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        listener.on(new UserHardDeletingEvent(userIds));

        verify(userActivityUseCase).usersHardDeleted(userIds);
    }

    @Test
    @DisplayName("활동 내역 갱신이 실패해도 예외를 밖으로 던지지 않는다.")
    void givenFailure_whenOn_thenDoesNotThrow() {
        doThrow(new IllegalStateException("Mongo 오류"))
                .when(userActivityUseCase)
                .usersHardDeleted(any());

        assertThatCode(() -> listener.on(new UserHardDeletingEvent(List.of(UUID.randomUUID()))))
                .doesNotThrowAnyException();
    }
}
