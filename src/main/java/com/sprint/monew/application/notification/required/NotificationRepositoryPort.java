package com.sprint.monew.application.notification.required;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.global.type.ResourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    Notification save(Notification entity);

    void saveAll(List<Notification> notifications);

    Optional<Notification> findById(UUID id);

    List<Notification> findAll();

    void deleteById(UUID id);

    List<Notification> findByQuery(UUID userId, NotificationQuery query);

    long countByQuery(UUID userId, NotificationQuery query);

    int markAllAsRead(UUID userId, LocalDateTime readAt);

    int deleteReadBefore(LocalDateTime threshold);

    // 리소스(댓글 등)가 물리 삭제될 때 그 리소스를 가리키는 알림을 한 번에 지우기용
    int deleteByResource(ResourceType resourceType, UUID resourceId);
}
