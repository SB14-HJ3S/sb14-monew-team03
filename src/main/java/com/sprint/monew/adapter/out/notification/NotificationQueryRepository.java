package com.sprint.monew.adapter.out.notification;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.domain.notification.entity.Notification;
import java.util.List;
import java.util.UUID;

public interface NotificationQueryRepository {

    List<Notification> findByQuery(UUID userId, NotificationQuery query);

    long countByQuery(UUID userId, NotificationQuery query);
}
