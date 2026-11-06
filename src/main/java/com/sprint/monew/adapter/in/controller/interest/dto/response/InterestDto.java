package com.sprint.monew.adapter.in.controller.interest.dto.response;

import com.sprint.monew.application.interest.provided.dto.InterestResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record InterestDto(
        @Schema(description = "관심사 ID") UUID id,
        @Schema(description = "관심사 이름") String name,
        @Schema(description = "관련 키워드 목록") List<String> keywords,
        @Schema(description = "구독자 수") Long subscriberCount,
        @Schema(description = "요청자의 구독 여부(키워드 수정 응답에서는 null)") Boolean subscribedByMe) {

    public static InterestDto from(InterestResult result) {
        return new InterestDto(
                result.id(),
                result.name(),
                result.keywords(),
                result.subscriberCount(),
                result.subscribedByMe());
    }
}
