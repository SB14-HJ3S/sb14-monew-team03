package com.sprint.monew.application.notification;

import com.sprint.monew.application.interest.ISubscriptionService;
import com.sprint.monew.application.notification.provided.NotificationUseCase;
import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.pagination.PageResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationApplication implements NotificationUseCase {

    private final NotificationService notificationService;
    private final ISubscriptionService subscriptionService;
    private final IUserService userService;

    @Override
    public PageResult<Notification> findByQuery(NotificationQuery query) {
        return null;
    }

    @Override
    public void markAllAsRead(UUID requestId) {}

    @Override
    public void markAsRead(UUID notificationId, UUID requestId) {}

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createArticleNotifications(UUID interestId, String interestName, int articleCount) {
        List<User> subscribers =
                subscriptionService.findSubscriberIds(interestId).stream()
                        .map(userService::getActiveUser)
                        .toList();

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
}
