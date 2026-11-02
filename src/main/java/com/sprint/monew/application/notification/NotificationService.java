package com.sprint.monew.application.notification;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ResourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void createArticleNotifications(
            List<User> users, UUID interestId, String interestName, int articleCount);

    void createCommentLikeNotification(User commentUser, UUID commentId, String likedByNickname);

    PageResult<Notification> findUnread(UUID userId, NotificationQuery query);

    int markAllAsRead(UUID userId);

    void markAsRead(UUID userId, UUID notificationId);

    int deleteReadBefore(LocalDateTime threshold);

    int deleteByResource(ResourceType resourceType, UUID resourceId);
}
