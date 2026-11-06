package com.sprint.monew.adapter.in.controller.notification.dto.response;

import com.sprint.monew.application.notification.provided.dto.NotificationResult;
import com.sprint.monew.global.type.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        @Schema(description = "알림 ID") UUID id,
        @Schema(description = "생성 시각") Instant createdAt,
        @Schema(description = "수정 시각") Instant updatedAt,
        @Schema(description = "확인 여부") boolean confirmed,
        @Schema(description = "알림 받는 사용자 ID") UUID userId,
        @Schema(description = "알림 내용", example = "축구와 관련된 기사가 3건 등록되었습니다.") String content,
        @Schema(description = "관련 리소스 종류", example = "INTEREST") ResourceType resourceType,
        @Schema(description = "관련 리소스 ID (관심사 또는 댓글)") UUID resourceId) {

    public static NotificationDto from(NotificationResult result) {
        return new NotificationDto(
                result.id(),
                result.createdAt(),
                result.updatedAt(),
                result.confirmed(),
                result.userId(),
                result.content(),
                result.resourceType(),
                result.resourceId());
    }
}
