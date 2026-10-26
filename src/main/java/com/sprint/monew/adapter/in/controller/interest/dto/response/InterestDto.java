package com.sprint.monew.adapter.in.controller.interest.dto.response;

import com.sprint.monew.application.interest.provided.dto.InterestResult;
import java.util.List;
import java.util.UUID;

public record InterestDto(
        UUID id, // 관심사 ID
        String name, // 관심사 이름
        List<String> keywords, // 관련 키워드 목록
        Long subscriberCount, // 구독자 수
        Boolean subscribedByMe // 요청자의 구독 여부
        ) {

    public static InterestDto from(InterestResult result) {
        return new InterestDto(
                result.id(),
                result.name(),
                result.keywords(),
                result.subscriberCount(),
                result.subscribedByMe());
    }
}
