package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.usecase.dto.SubscriptionActivityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "구독 정보")
public record SubscriptionActivityDto(
        @Schema(description = "구독 정보 ID") UUID id,
        @Schema(description = "관심사 ID") UUID interestId,
        @Schema(description = "관심사 이름") String interestName,
        @Schema(description = "관련 키워드 목록") List<String> interestKeywords,
        @Schema(description = "구독자 수") long interestSubscriberCount,
        @Schema(description = "구독한 날짜") Instant createdAt) {
    public static SubscriptionActivityDto from(SubscriptionActivityResult r) {
        return new SubscriptionActivityDto(
                r.id(),
                r.interestId(),
                r.interestName(),
                r.interestKeywords(),
                r.interestSubscriberCount(),
                r.createdAt());
    }
}
