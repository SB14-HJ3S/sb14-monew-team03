package com.sprint.monew.application.notification.provided;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.global.pagination.PageResult;
import java.util.UUID;

public interface NotificationUseCase {
    PageResult<Notification> findByQuery(NotificationQuery query);

    void markAllAsRead(UUID requestId);

    void markAsRead(UUID notificationId, UUID requestId);
}
