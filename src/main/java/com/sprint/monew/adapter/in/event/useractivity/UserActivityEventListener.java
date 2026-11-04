package com.sprint.monew.adapter.in.event.useractivity;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 다른 도메인의 활동 이벤트를 받아 활동 내역(MongoDB)에 반영한다. - AFTER_COMMIT: 원래 작업(PostgreSQL)이 확정된 경우에만 반영 (롤백되면
 * Mongo도 건드리지 않음) - @Async: Mongo 작업이 요청·배치 스레드를 붙잡지 않도록 별도 스레드에서 처리 - 실패해도 원래 작업엔 영향이 없으므로 이벤트와 함께
 * 로그만 남긴다
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Async("userActivityExecutor")
public class UserActivityEventListener {

    private final UserActivityUseCase userActivityUseCase;

    // ===== 사용자 =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredEvent e) {
        run(
                e,
                () ->
                        userActivityUseCase.userRegistered(
                                e.userId(), e.email(), e.nickname(), e.createdAt()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserNicknameUpdatedEvent e) {
        run(e, () -> userActivityUseCase.nicknameUpdated(e.userId(), e.nickname()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserHardDeletingEvent e) {
        run(e, () -> userActivityUseCase.usersHardDeleted(e.userIds()));
    }

    // ===== 구독 관심사 =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InterestSubscribedEvent e) {
        run(
                e,
                () ->
                        userActivityUseCase.subscribed(
                                e.userId(),
                                new SubscriptionActivityResult(
                                        e.subscriptionId(),
                                        e.interestId(),
                                        e.interestName(),
                                        e.interestKeywords(),
                                        e.interestSubscriberCount(),
                                        e.createdAt())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InterestUnsubscribedEvent e) {
        run(e, () -> userActivityUseCase.unsubscribed(e.userId(), e.interestId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InterestKeywordsUpdatedEvent e) {
        run(e, () -> userActivityUseCase.interestKeywordsUpdated(e.interestId(), e.keywords()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InterestDeletedEvent e) {
        run(e, () -> userActivityUseCase.interestDeleted(e.interestId()));
    }

    // ===== 댓글 =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentCreatedEvent e) {
        run(
                e,
                () ->
                        userActivityUseCase.commentCreated(
                                e.userId(),
                                new CommentActivityResult(
                                        e.commentId(),
                                        e.articleId(),
                                        e.articleTitle(),
                                        e.userId(),
                                        e.userNickname(),
                                        e.content(),
                                        e.likeCount(),
                                        e.createdAt())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentUpdatedEvent e) {
        run(e, () -> userActivityUseCase.commentUpdated(e.commentId(), e.content()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentDeletedEvent e) {
        run(e, () -> userActivityUseCase.commentDeleted(e.commentId()));
    }

    // ===== 좋아요 =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentLikeCreatedEvent e) {
        run(
                e,
                () ->
                        userActivityUseCase.commentLiked(
                                e.likedById(),
                                new CommentLikeActivityResult(
                                        e.likeId(),
                                        e.likedAt(),
                                        e.commentId(),
                                        e.articleId(),
                                        e.articleTitle(),
                                        e.commentUserId(),
                                        e.commentUserNickname(),
                                        e.commentContent(),
                                        e.commentLikeCount(),
                                        e.commentCreatedAt())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentUnlikedEvent e) {
        run(
                e,
                () ->
                        userActivityUseCase.commentUnliked(
                                e.likedById(), e.commentId(), e.commentLikeCount()));
    }

    // ===== 기사 =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ArticleViewedEvent e) {
        run(
                e,
                () ->
                        userActivityUseCase.articleViewed(
                                e.userId(),
                                new ArticleViewActivityResult(
                                        e.viewId(),
                                        e.userId(),
                                        e.viewedAt(),
                                        e.articleId(),
                                        e.source(),
                                        e.sourceUrl(),
                                        e.articleTitle(),
                                        e.articlePublishedDate(),
                                        e.articleSummary(),
                                        e.articleCommentCount(),
                                        e.articleViewCount())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ArticleSoftDeletedEvent e) {
        run(e, () -> userActivityUseCase.articleDeleted(e.articleId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ArticleHardDeletingEvent e) {
        // 논리 삭제 때 이미 빠졌으면 $pull이 아무것도 하지 않는다
        run(e, () -> userActivityUseCase.articleDeleted(e.articleId()));
    }

    // ===== 공통 =====

    private void run(Object event, Runnable action) {
        try {
            log.debug("활동 내역 갱신 스레드: {}, 이벤트: {}", Thread.currentThread().getName(), event);
            action.run();
        } catch (Exception ex) {
            log.error("활동 내역 갱신 실패: {}", event, ex);
        }
    }
}
