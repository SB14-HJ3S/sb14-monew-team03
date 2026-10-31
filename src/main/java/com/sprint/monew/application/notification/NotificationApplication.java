package com.sprint.monew.application.notification;

import com.sprint.monew.application.interest.ISubscriptionService;
import com.sprint.monew.application.notification.provided.NotificationUseCase;
import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.application.notification.provided.dto.NotificationResult;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.pagination.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ApplicationService
@RequiredArgsConstructor
public class NotificationApplication implements NotificationUseCase {

    private static final int READ_NOTIFICATION_RETENTION_DAYS = 7;

    private final NotificationService notificationService;
    private final ISubscriptionService subscriptionService;
    private final IUserService userService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<NotificationResult> findByQuery(NotificationQuery query) {
        UUID userId = RequestIdContext.get();
        return notificationService.findUnread(userId, query).map(NotificationResult::from);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        UUID userId = RequestIdContext.get();
        userService.getActiveUser(userId);

        int updated = notificationService.markAllAsRead(userId);
        log.info("알림 전체 확인: userId={}, 확인된 알림 수={}", userId, updated);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        UUID userId = RequestIdContext.get();
        userService.getActiveUser(userId);
        notificationService.markAsRead(userId, notificationId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createArticleNotifications(UUID interestId, String interestName, int articleCount) {
        List<UUID> subscriberIds = subscriptionService.findSubscriberIds(interestId);
        List<User> subscribers = userService.getActiveUsers(subscriberIds);

        notificationService.createArticleNotifications(
                subscribers, interestId, interestName, articleCount);
        log.info("기사 알림 생성: interestId={}, 대상={}명", interestId, subscribers.size());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createCommentLikeNotification(
            UUID commentId, UUID commentUserId, UUID likedById, String likedByNickname) {

        if (commentUserId.equals(likedById)) {
            return;
        }

        User commentUser = userService.getActiveUser(commentUserId);
        notificationService.createCommentLikeNotification(commentUser, commentId, likedByNickname);
        log.info("좋아요 알림 생성: commentId={}, commentUserId={}", commentId, commentUserId);
    }

    @Override
    @Transactional
    public int deleteOldReadNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(READ_NOTIFICATION_RETENTION_DAYS);
        int deleted = notificationService.deleteReadBefore(threshold);
        log.info(
                "확인 후 {}일 지난 알림 삭제: {}건 (기준 시각={})",
                READ_NOTIFICATION_RETENTION_DAYS,
                deleted,
                threshold);
        return deleted;
    }
}
