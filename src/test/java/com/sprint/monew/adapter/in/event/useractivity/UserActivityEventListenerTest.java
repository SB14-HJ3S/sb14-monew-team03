package com.sprint.monew.adapter.in.event.useractivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.article.event.ArticleHardDeletingEvent;
import com.sprint.monew.application.article.event.ArticleSoftDeletedEvent;
import com.sprint.monew.application.article.event.ArticleViewedEvent;
import com.sprint.monew.application.comment.event.CommentCreatedEvent;
import com.sprint.monew.application.comment.event.CommentDeletedEvent;
import com.sprint.monew.application.comment.event.CommentLikeCreatedEvent;
import com.sprint.monew.application.comment.event.CommentUnlikedEvent;
import com.sprint.monew.application.comment.event.CommentUpdatedEvent;
import com.sprint.monew.application.interest.event.InterestDeletedEvent;
import com.sprint.monew.application.interest.event.InterestKeywordsUpdatedEvent;
import com.sprint.monew.application.interest.event.InterestSubscribedEvent;
import com.sprint.monew.application.interest.event.InterestUnsubscribedEvent;
import com.sprint.monew.application.user.event.UserHardDeletingEvent;
import com.sprint.monew.application.user.event.UserNicknameUpdatedEvent;
import com.sprint.monew.application.user.event.UserRegisteredEvent;
import com.sprint.monew.application.useractivity.provided.UserActivityUseCase;
import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import com.sprint.monew.global.type.ArticleSourceType;
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

        verify(userActivityUseCase).commentCreated(eq(userId), commentCaptor.capture());
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

    @Test
    @DisplayName("가입 이벤트를 받으면 활동 내역 문서를 만든다.")
    void givenUserRegistered_whenOn_thenUserRegistered() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        listener.on(new UserRegisteredEvent(userId, "woody@monew.com", "우디", createdAt));

        verify(userActivityUseCase).userRegistered(userId, "woody@monew.com", "우디", createdAt);
    }

    @Test
    @DisplayName("닉네임 변경 이벤트를 받으면 닉네임을 갱신한다.")
    void givenNicknameUpdated_whenOn_thenNicknameUpdated() {
        UUID userId = UUID.randomUUID();

        listener.on(new UserNicknameUpdatedEvent(userId, "버즈"));

        verify(userActivityUseCase).nicknameUpdated(userId, "버즈");
    }

    @Test
    @DisplayName("구독 이벤트를 받으면 구독 항목을 추가한다.")
    void givenSubscribed_whenOn_thenSubscribed() {
        UUID userId = UUID.randomUUID();

        listener.on(
                new InterestSubscribedEvent(
                        UUID.randomUUID(),
                        userId,
                        UUID.randomUUID(),
                        "축구",
                        List.of("손흥민"),
                        1L,
                        Instant.now()));

        verify(userActivityUseCase).subscribed(eq(userId), any(SubscriptionActivityResult.class));
    }

    @Test
    @DisplayName("구독 취소 이벤트를 받으면 구독 항목을 뺀다.")
    void givenUnsubscribed_whenOn_thenUnsubscribed() {
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        listener.on(new InterestUnsubscribedEvent(userId, interestId));

        verify(userActivityUseCase).unsubscribed(userId, interestId);
    }

    @Test
    @DisplayName("키워드 수정 이벤트를 받으면 구독자 문서의 키워드를 바꾼다.")
    void givenKeywordsUpdated_whenOn_thenKeywordsUpdated() {
        UUID interestId = UUID.randomUUID();

        listener.on(new InterestKeywordsUpdatedEvent(interestId, List.of("EPL")));

        verify(userActivityUseCase).interestKeywordsUpdated(interestId, List.of("EPL"));
    }

    @Test
    @DisplayName("관심사 삭제 이벤트를 받으면 구독 목록에서 뺀다.")
    void givenInterestDeleted_whenOn_thenInterestDeleted() {
        UUID interestId = UUID.randomUUID();

        listener.on(new InterestDeletedEvent(interestId));

        verify(userActivityUseCase).interestDeleted(interestId);
    }

    @Test
    @DisplayName("댓글 수정 이벤트를 받으면 댓글 내용을 바꾼다.")
    void givenCommentUpdated_whenOn_thenCommentUpdated() {
        UUID commentId = UUID.randomUUID();

        listener.on(new CommentUpdatedEvent(commentId, "수정"));

        verify(userActivityUseCase).commentUpdated(commentId, "수정");
    }

    @Test
    @DisplayName("댓글 삭제 이벤트를 받으면 댓글을 뺀다.")
    void givenCommentDeleted_whenOn_thenCommentDeleted() {
        UUID commentId = UUID.randomUUID();

        listener.on(new CommentDeletedEvent(commentId));

        verify(userActivityUseCase).commentDeleted(commentId);
    }

    @Test
    @DisplayName("좋아요 이벤트를 받으면 좋아요 누른 사람 문서에 추가한다.")
    void givenCommentLikeCreated_whenOn_thenCommentLiked() {
        UUID likedById = UUID.randomUUID();

        listener.on(
                new CommentLikeCreatedEvent(
                        UUID.randomUUID(),
                        likedById,
                        Instant.now(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "기사",
                        UUID.randomUUID(),
                        "버즈",
                        "내용",
                        1L,
                        Instant.now()));

        verify(userActivityUseCase)
                .commentLiked(eq(likedById), any(CommentLikeActivityResult.class));
    }

    @Test
    @DisplayName("좋아요 취소 이벤트를 받으면 좋아요 항목을 뺀다.")
    void givenCommentUnliked_whenOn_thenCommentUnliked() {
        UUID commentId = UUID.randomUUID();
        UUID likedById = UUID.randomUUID();

        listener.on(new CommentUnlikedEvent(commentId, likedById, 3L));

        verify(userActivityUseCase).commentUnliked(likedById, commentId, 3L);
    }

    @Test
    @DisplayName("기사 조회 이벤트를 받으면 본 기사에 추가한다.")
    void givenArticleViewed_whenOn_thenArticleViewed() {
        UUID userId = UUID.randomUUID();

        listener.on(
                new ArticleViewedEvent(
                        UUID.randomUUID(),
                        userId,
                        UUID.randomUUID(),
                        ArticleSourceType.NAVER,
                        "https://a.com",
                        "기사",
                        Instant.now(),
                        "요약",
                        0L,
                        1L,
                        Instant.now()));

        verify(userActivityUseCase).articleViewed(eq(userId), any(ArticleViewActivityResult.class));
    }

    @Test
    @DisplayName("기사 논리·물리 삭제 이벤트를 받으면 그 기사 항목을 뺀다.")
    void givenArticleDeleted_whenOn_thenArticleDeleted() {
        UUID articleId = UUID.randomUUID();

        listener.on(new ArticleSoftDeletedEvent(articleId));
        listener.on(new ArticleHardDeletingEvent(articleId));

        verify(userActivityUseCase, times(2)).articleDeleted(articleId);
    }
}
