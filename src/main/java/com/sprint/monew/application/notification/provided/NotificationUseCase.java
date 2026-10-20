package com.sprint.monew.application.notification.provided;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.domain.notification.entity.Notification;
import java.util.UUID;

public interface NotificationUseCase {
    Notification findByQuery(NotificationQuery query);

    void markAllAsRead(UUID requestId);

    void markAsRead(UUID notificationId, UUID requestId);
}
