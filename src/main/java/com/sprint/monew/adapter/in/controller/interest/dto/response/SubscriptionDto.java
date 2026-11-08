package com.sprint.monew.adapter.in.controller.interest.dto.response;

import com.sprint.monew.application.interest.provided.usecase.dto.SubscriptionResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto(
        @Schema(description = "구독 정보 ID") UUID id,
        @Schema(description = "관심사 ID") UUID interestId,
        @Schema(description = "관심사 이름") String interestName,
        @Schema(description = "관련 키워드 목록") List<String> interestKeywords,
        @Schema(description = "관심사 구독자 수") Long interestSubscriberCount,
        @Schema(description = "구독한 날짜") Instant createdAt) {
    public static SubscriptionDto from(SubscriptionResult result) {
        return new SubscriptionDto(
                result.id(),
                result.interestId(),
                result.interestName(),
                result.interestKeywords(),
                result.subscriberCount(),
                result.createdAt());
    }
}
