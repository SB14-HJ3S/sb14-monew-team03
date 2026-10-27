package com.sprint.monew.adapter.in.controller.notification.dto.response;

import com.sprint.monew.application.notification.provided.dto.NotificationResult;
import com.sprint.monew.global.type.ResourceType;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id, // 알림 ID
        Instant createdAt, // 생성된 날짜
        Instant updatedAt, // 확인한 날짜
        boolean confirmed, // 확인 여부
        UUID userId, // 알림 대상 사용자 ID
        String content, // 내용
        ResourceType resourceType, // 관련된 리소스 유형
        UUID resourceId) {

    public static NotificationDto from(NotificationResult result) {
        return new NotificationDto(
                result.id(),
                result.createdAt(),
                result.createdAt(),
                result.confirmed(),
                result.userId(),
                result.content(),
                result.resourceType(),
                result.resourceId());
    }
}
