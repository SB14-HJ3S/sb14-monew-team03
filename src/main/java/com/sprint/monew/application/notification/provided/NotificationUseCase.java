package com.sprint.monew.application.notification.provided;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.application.notification.provided.dto.NotificationResult;
import com.sprint.monew.global.pagination.PageResult;
import java.util.UUID;

public interface NotificationUseCase {
    PageResult<NotificationResult> findByQuery(NotificationQuery query);

    void markAllAsRead(UUID requestId);

    void markAsRead(UUID notificationId, UUID requestId);

    void createArticleNotifications(UUID interestId, String interestName, int articleCount);

    void createCommentLikeNotification(
            UUID commentId, UUID commentUserId, UUID likedById, String likedByNickname);
}
