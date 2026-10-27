package com.sprint.monew.application.notification.provided.dto;

import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.global.type.ResourceType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public record NotificationResult(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        boolean confirmed,
        UUID userId,
        String content,
        ResourceType resourceType,
        UUID resourceId) {
    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.getId(),
                notification.getCreatedAt(),
                updatedAtOf(notification),
                notification.isRead(),
                notification.getUser().getId(),
                notification.getContent(),
                notification.getResourceType(),
                notification.getResourceId());
    }

    private static Instant updatedAtOf(Notification notification) {
        if (notification.getReadAt() == null) {
            return notification.getCreatedAt();
        }
        return notification.getReadAt().atZone(ZoneId.systemDefault()).toInstant();
    }
}
